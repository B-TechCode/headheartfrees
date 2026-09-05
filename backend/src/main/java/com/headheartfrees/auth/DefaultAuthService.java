package com.headheartfrees.auth;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The auth module's behaviour. Package-private; callers hold {@link AuthService}.
 */
@Service
class DefaultAuthService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAuthService.class);

    private final UserAccountRepository users;
    private final RefreshTokenService refreshTokens;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    DefaultAuthService(
            UserAccountRepository users,
            RefreshTokenService refreshTokens,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Registers, or convincingly appears to.
     *
     * <h2>Why a duplicate email returns normally</h2>
     *
     * A 409 on a taken address turns this endpoint into an account-existence
     * oracle: anyone can test any address and learn whether that person has an
     * account here. On a site about mental health that is not an abstract
     * privacy concern - the answer is disclosive on its own, regardless of what
     * the person ever wrote.
     *
     * <p>So both paths return the same status and the same body, and the
     * controller's response text is written to be true in either case.
     *
     * <h2>Why the hash still runs on the duplicate path</h2>
     *
     * Argon2id at these parameters takes tens of milliseconds. Skipping it when
     * the address is taken would make the duplicate path measurably faster than
     * the new-account path, and the difference is large enough to read over a
     * network - which restores exactly the oracle the shared response removes.
     * The hash is computed and discarded. That waste is the point.
     *
     * <p>What this cannot fix is the user's experience: someone who forgot they
     * had an account is told nothing useful. Closing that needs either an email
     * ("you already have an account") or a message that admits both
     * possibilities. There is no mail transport here, so phase 6 takes the
     * second option - the success screen must say that the account is ready if
     * the address is new and to sign in if it is not. Recorded in the phase 5
     * log as a phase 6 requirement.
     */
    @Override
    @Transactional
    public void register(String email, String password, String displayName) {
        String normalisedEmail = email == null ? "" : email.trim();
        PasswordPolicy.validate(password, normalisedEmail, displayName);

        // Always. See the class comment - this is a timing countermeasure, not
        // a value that is always used.
        String hash = passwordEncoder.encode(password);

        if (users.existsByEmail(normalisedEmail)) {
            // Deliberately not logged at WARN or above, and the address is not
            // logged at all: a log line per duplicate registration attempt is
            // an account-existence oracle for anyone who can read logs.
            log.debug("Registration attempt on an existing address; returning the shared response");
            return;
        }

        UserAccount account = UserAccount.withPassword(
                normalisedEmail, hash, trimToNull(displayName), clock.instant());
        users.save(account);
        log.info("Registered account {}", account.getId());
    }

    @Override
    @Transactional
    public TokenPair login(String email, String password) {
        Optional<UserAccount> found = users.findByEmail(email == null ? "" : email.trim());

        if (found.isEmpty()) {
            // Hash against a throwaway value so that an unknown address costs
            // the same as a known one. Without this, "no such user" returns in
            // microseconds and "wrong password" in tens of milliseconds, and
            // the generic error message stops mattering.
            passwordEncoder.encode(password == null ? "" : password);
            throw new InvalidCredentialsException();
        }

        UserAccount account = found.get();
        String storedHash = account.getPasswordHash();

        if (storedHash == null) {
            // A Google-only account. Must not be treated as "no password set,
            // so anything matches", and must not say so either - reporting
            // "this account uses Google" would confirm the address exists.
            passwordEncoder.encode(password == null ? "" : password);
            throw new InvalidCredentialsException();
        }

        if (password == null || !passwordEncoder.matches(password, storedHash)) {
            throw new InvalidCredentialsException();
        }

        return issueFor(account, refreshTokens.issueNewFamily(account.getId()));
    }

    @Override
    // noRollbackFor: the reuse path deliberately commits a family-wide
    // revocation and then throws. See RefreshTokenService.rotate - both layers
    // need the exclusion, because they share one physical transaction.
    @Transactional(noRollbackFor = RefreshTokenReuseException.class)
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidCredentialsException();
        }
        // Throws RefreshTokenReuseException, having revoked the family, when
        // the token was already spent.
        RefreshTokenService.IssuedToken issued = refreshTokens.rotate(refreshToken);

        // The account can be missing if it was deleted while a token was still
        // live. The new token is already written at this point, which is
        // harmless - it belongs to a user id that no longer resolves, so it can
        // never authenticate anything.
        UserAccount account = users.findById(issued.userId())
                .orElseThrow(InvalidCredentialsException::new);

        return issueFor(account, issued);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.revokeFamilyOf(refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummary summarise(UUID userId) {
        return users.findById(userId)
                .map(UserAccount::toSummary)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private TokenPair issueFor(UserAccount account, RefreshTokenService.IssuedToken refresh) {
        String access = jwtService.issueAccessToken(account.getId(), account.getRole());
        return new TokenPair(
                access,
                jwtService.accessTokenTtl(),
                refresh.token(),
                refresh.ttl(),
                account.toSummary());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
