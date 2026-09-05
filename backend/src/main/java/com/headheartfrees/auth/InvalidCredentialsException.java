package com.headheartfrees.auth;

/**
 * One exception for every way authentication can fail.
 *
 * <p>There is deliberately no subtype for "no such user" versus "wrong
 * password" versus "this account signs in with Google". Distinguishing them in
 * the type system is how the distinction eventually reaches a response body -
 * somebody adds a handler per subtype with a helpful message, and the API
 * becomes an account-existence oracle. One type, one message.
 */
public class InvalidCredentialsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
