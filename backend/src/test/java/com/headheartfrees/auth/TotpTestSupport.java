package com.headheartfrees.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Fixtures for the second-factor tests.
 *
 * <h2>The codes are generated independently of the code under test</h2>
 *
 * {@link #codeFor} builds its own {@link TimeBasedOneTimePasswordGenerator}
 * from the base32 key the setup endpoint printed for a human to type. It does
 * not reach into {@code TotpService}, borrow its generator, or ask it what the
 * right answer is. That matters: a test that obtained the expected code from
 * the same object that validates it would pass even if both were wrong in the
 * same way, which is exactly what happens when somebody gets the base32 bit
 * order backwards.
 *
 * <p>Going through the published {@code manualKey} also means these tests
 * exercise the real enrolment path an authenticator app takes - decode the
 * printed key, derive a code, present it - rather than a shortcut past it.
 */
abstract class TotpTestSupport extends AuthTestSupport {

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private UserTotpRepository totps;

    @Autowired
    private TotpBackupCodeRepository backupCodeRepository;

    private final TimeBasedOneTimePasswordGenerator generator =
            new TimeBasedOneTimePasswordGenerator();

    /**
     * Clears the TOTP tables too.
     *
     * <p>{@code users} cascades to both, so this is belt and braces - but a
     * test that depended on the cascade would stop testing anything the day
     * somebody changed the foreign key.
     */
    @org.junit.jupiter.api.BeforeEach
    void resetTotpState() {
        backupCodeRepository.deleteAll();
        totps.deleteAll();
    }

    /** How many recovery codes this account has left, read straight from the table. */
    protected long unusedBackupCodesFor(String email) {
        UserAccount account = accounts.findByEmail(email).orElseThrow();
        return backupCodeRepository.countByUserIdAndUsedAtIsNull(account.getId());
    }

    /** Promotes an account the way {@code AdminBootstrap} does at startup. */
    protected void promoteToAdmin(String email) {
        UserAccount account = accounts.findByEmail(email).orElseThrow();
        account.assignRole(UserRole.ADMIN);
        accounts.save(account);
    }

    /** A six-digit code for {@code base32Key}, valid at this instant. */
    protected String codeFor(String base32Key) {
        return codeFor(base32Key, Instant.now());
    }

    protected String codeFor(String base32Key, Instant at) {
        try {
            return generator.generateOneTimePasswordString(
                    new SecretKeySpec(Base32.decode(base32Key), "HmacSHA1"), at);
        } catch (java.security.InvalidKeyException e) {
            throw new AssertionError("The published manual key is not a usable HMAC key", e);
        }
    }

    /** The instant one step later, for testing that a spent step stays spent. */
    protected Instant oneStepAfter(Instant at) {
        return at.plus(generator.getTimeStep());
    }

    /**
     * The instant one step earlier - still inside the drift window, so a code
     * derived here is refused only by the spent-step rule.
     */
    protected Instant oneStepBefore(Instant at) {
        return at.minus(generator.getTimeStep());
    }

    /**
     * A code usable for signing in immediately after enrolling.
     *
     * <h2>Why this is not just {@code codeFor(manualKey)}</h2>
     *
     * Enrolment verifies a code, and a verified code marks its time step spent.
     * A sign-in within the next thirty seconds derives the <em>same</em> step,
     * and the replay rule refuses it - correctly. So these tests take the next
     * step's code, which the drift window accepts and whose step is ahead of the
     * one enrolment used.
     *
     * <p>That is real behaviour rather than a test artefact: somebody who
     * enrols and then immediately signs in on a second device waits for the
     * digits to roll over. It is the ordinary cost of refusing replay, it is
     * invisible in the normal flow because enrolment already hands back a
     * session, and it is written down here so the next person does not read it
     * as a bug.
     */
    protected String signInCodeFor(Enrolled enrolled) {
        return codeFor(enrolled.manualKey(), oneStepAfter(Instant.now()));
    }

    protected ResultActions completeSignIn(String ticket, String code) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login/totp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("ticket", ticket, "code", code)));
    }

    /** Begins enrolment with a session, returning the published manual key. */
    protected String beginSetup(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/totp/setup")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("manualKey")
                .asText();
    }

    /** Begins enrolment with an enrolment ticket instead of a session. */
    protected String beginSetupWithTicket(String ticket) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/totp/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ticket", ticket)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("manualKey")
                .asText();
    }

    /**
     * Registers, signs in, enrols, and returns everything the tests need.
     *
     * <p>Resets the rate limiter afterwards: enrolling costs several calls
     * against the 5/min auth bucket and the 10/min TOTP bucket, and a test
     * about replay must not fail because getting to the starting line used up
     * the allowance.
     */
    protected Enrolled enrolUser(String email) throws Exception {
        MvcResult login = registerAndLogin(email);
        String accessToken = accessTokenOf(login);

        String manualKey = beginSetup(accessToken);

        MvcResult enabled = mockMvc.perform(post("/api/v1/auth/totp/enable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("code", codeFor(manualKey))))
                .andExpect(status().isOk())
                .andReturn();

        List<String> codes = new ArrayList<>();
        JsonNode body = objectMapper.readTree(enabled.getResponse().getContentAsString());
        body.get("backupCodes").forEach(node -> codes.add(node.asText()));

        resetRateLimiter();
        return new Enrolled(email, manualKey, List.copyOf(codes));
    }

    /** Builds a small JSON object without hand-escaping strings in each test. */
    protected String json(String... keysAndValues) throws Exception {
        var map = new java.util.LinkedHashMap<String, String>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return objectMapper.writeValueAsString(map);
    }

    /**
     * @param manualKey   the base32 secret, as an authenticator would receive it
     * @param backupCodes the ten codes, as they were shown once
     */
    protected record Enrolled(String email, String manualKey, List<String> backupCodes) {
    }
}
