package com.headheartfrees.auth;

/**
 * The single response to every registration attempt.
 *
 * <p>Identical whether an account was created or the address was already taken.
 * The wording has to be true in both cases, which is why it is conditional
 * rather than congratulatory - "your account is ready" alone would be a lie on
 * the duplicate path, and a lie a returning user would act on.
 *
 * <p>Phase 6 must render this alongside a link to /login. See the phase 5 log.
 */
public record RegistrationResponse(String message) {

    static final String SHARED_MESSAGE =
            "If that address is new, your account is ready. "
                    + "If you already had one, sign in instead.";

    static RegistrationResponse shared() {
        return new RegistrationResponse(SHARED_MESSAGE);
    }
}
