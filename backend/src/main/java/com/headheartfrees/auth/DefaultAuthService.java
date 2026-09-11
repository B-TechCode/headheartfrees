package com.headheartfrees.auth;

import com.headheartfrees.config.TotpProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
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
    private final TotpService totp;
    private final Clock clock;
    private final Duration challengeTtl;

    DefaultAuthService(
            UserAccountRepository users,
            RefreshTokenService refreshTokens,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            TotpService totp,
            Clock clock,
            TotpProperties totpProperties) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.totp = totp;
        this.clock = clock;
        this.challengeTtl = totpProperties.challengeTtl();
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

    /**
     * Password first, then - for anyone who owes one - a code.
     *
     * <h2>The ordering is the guarantee</h2>
     *
     * Nothing below the password check issues a token. The two {@code return}s
     * that are not {@link LoginOutcome.Authenticated} hand back a ticket that
     * {@link JwtAuthenticationFilter} refuses as a Bearer token, so there is no
     * path from here to an authenticated request without a second factor
     * having been presented.
     *
     * <h2>Why an unenrolled ADMIN gets a ticket rather than a refusal</h2>
     *
     * Because the alternative locks out every admin on the day this deploys,
     * and this install has exactly one. See
     * {@link LoginOutcome.EnrolmentRequired}.
     */
    @Override
    @Transactional
    public LoginOutcome login(String email, String password) {
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

        return afterPasswordAccepted(account);
    }

    /**
     * The one place that decides whether a verified password is enough.
     *
     * <p>Reached from {@link #login} only, because every other route to a
     * session is either refused outright or forced back through it:
     * {@link GoogleSignInHandler} refuses ADMIN accounts, and {@link #refresh}
     * revokes an ADMIN family that owes a factor.
     */
    private LoginOutcome afterPasswordAccepted(UserAccount account) {
        if (totp.isEnrolled(account.getId())) {
            return new LoginOutcome.SecondFactorRequired(
                    jwtService.issueTicket(
                            account.getId(), JwtService.TYPE_TOTP_CHALLENGE, challengeTtl),
                    challengeTtl);
        }

        if (TotpService.isRequiredFor(account.getRole())) {
            log.info("Account {} must enrol a second factor before signing in", account.getId());
            return new LoginOutcome.EnrolmentRequired(
                    jwtService.issueTicket(
                            account.getId(), JwtService.TYPE_TOTP_ENROLMENT, challengeTtl),
                    challengeTtl);
        }

        return new LoginOutcome.Authenticated(
                issueFor(account, refreshTokens.issueNewFamily(account.getId())));
    }

    @Override
    // noRollbackFor: the failure path deliberately commits an incremented
    // attempt counter and then throws. See TotpService.verifySecondFactor -
    // both layers need the exclusion, because they share one physical
    // transaction and either marking it rollback-only loses the write, which
    // turns the attempt limit into decoration.
    @Transactional(noRollbackFor = InvalidTotpCodeException.class)
    public TokenPair completeSecondFactor(String ticket, String code) {
        UUID userId = jwtService.verifyTicket(ticket, JwtService.TYPE_TOTP_CHALLENGE)
                .orElseThrow(InvalidTotpCodeException::new);

        // Throws on every failure, and counts the ones that were guesses.
        totp.verifySecondFactor(userId, code);

        UserAccount account = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        return issueFor(account, refreshTokens.issueNewFamily(userId));
    }

    @Override
    public UUID resolveEnrolmentTicket(String ticket) {
        return jwtService.verifyTicket(ticket, JwtService.TYPE_TOTP_ENROLMENT)
                .orElseThrow(InvalidTotpCodeException::new);
    }

    @Override
    @Transactional
    public TotpService.TotpSetup beginTotpSetup(UUID userId) {
        UserAccount account = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        return totp.beginSetup(userId, account.getEmail());
    }

    @Override
    @Transactional(noRollbackFor = InvalidTotpCodeException.class)
    public TotpEnableResult enableTotp(UUID userId, String code, boolean issueSession) {
        List<String> backupCodes = totp.enable(userId, code);

        if (!issueSession) {
            return new TotpEnableResult(backupCodes, null);
        }

        UserAccount account = users.findById(userId).orElseThrow(InvalidCredentialsException::new);
        return new TotpEnableResult(
                backupCodes, issueFor(account, refreshTokens.issueNewFamily(userId)));
    }

    @Override
    @Transactional(noRollbackFor = InvalidTotpCodeException.class)
    public List<String> regenerateBackupCodes(UUID userId, String code) {
        return totp.regenerateBackupCodes(userId, code);
    }

    /**
     * Password <em>and</em> a current code, both.
     *
     * <p>Requiring the password too is the point. The threat this guards is a
     * session somebody else is holding - a borrowed laptop, a stolen access
     * token - and a session is exactly what an attacker in that position
     * already has. Without the password, turning the second factor off would be
     * the one move a hijacked session could make to render itself permanent.
     */
    @Override
    @Transactional(noRollbackFor = InvalidTotpCodeException.class)
    public void disableTotp(UUID userId, String password, String code) {
        UserAccount account = users.findById(userId).orElseThrow(InvalidCredentialsException::new);

        String storedHash = account.getPasswordHash();
        if (storedHash == null
                || password == null
                || !passwordEncoder.matches(password, storedHash)) {
            throw new InvalidCredentialsException();
        }

        // The code is verified before the role is checked, so a USER and an
        // ADMIN take the same path to the same point. Checking the role first
        // would let an admin learn the action is refused without presenting
        // anything - not a secret, but the ordering costs nothing and keeps one
        // shape.
        totp.verifySecondFactor(userId, code);
        totp.disable(userId, account.getRole());

        // Everything issued to another device is revoked. Weakening an
        // account's sign-in is a security event, and leaving other sessions
        // live would mean the weakened account is still reachable from wherever
        // it was already signed in.
        refreshTokens.revokeAllFor(userId);
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

        // Closing the deploy-day hole. See AuthService.refresh for why it is
        // here rather than left to the next sign-in: a refresh cookie issued
        // before this feature existed is otherwise thirty days of admin access
        // that never meets the requirement.
        //
        // AFTER rotate(), deliberately. Rotation has already spent the
        // presented token, so this cannot be worked around by presenting it
        // again - that path is reuse detection, which revokes the family too.
        if (TotpService.isRequiredFor(account.getRole()) && !totp.isEnrolled(account.getId())) {
            log.info(
                    "Revoking sessions for admin account {}: it holds no second factor, so this "
                            + "refresh family predates the requirement and must not outlive it",
                    account.getId());
            refreshTokens.revokeAllFor(account.getId());
            throw new InvalidCredentialsException();
        }

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
                .map(this::summarise)
                .orElseThrow(InvalidCredentialsException::new);
    }

    /**
     * The one place a {@link UserSummary} is built, so the second-factor state
     * on it cannot be current in one response and stale in another.
     */
    private UserSummary summarise(UserAccount account) {
        return account.toSummary(
                totp.isEnrolled(account.getId()), totp.remainingBackupCodes(account.getId()));
    }

    private TokenPair issueFor(UserAccount account, RefreshTokenService.IssuedToken refresh) {
        String access = jwtService.issueAccessToken(account.getId(), account.getRole());
        return new TokenPair(
                access,
                jwtService.accessTokenTtl(),
                refresh.token(),
                refresh.ttl(),
                summarise(account));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
