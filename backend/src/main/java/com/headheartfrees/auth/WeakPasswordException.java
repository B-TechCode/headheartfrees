package com.headheartfrees.auth;

/**
 * The password failed {@link PasswordPolicy}. Rendered as 400 with the reason,
 * which is safe to state: it says something about the submitted password, not
 * about whether an account exists.
 */
public class WeakPasswordException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public WeakPasswordException(String reason) {
        super(reason);
    }
}
