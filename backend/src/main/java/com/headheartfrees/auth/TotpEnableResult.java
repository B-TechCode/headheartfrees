package com.headheartfrees.auth;

import java.util.List;

/**
 * The result of turning the second factor on.
 *
 * <p>Always the backup codes. Additionally a session, but only when enrolment
 * was reached by an {@link JwtService#TYPE_TOTP_ENROLMENT} ticket - that is,
 * when an admin was sent here instead of being signed in. Somebody who enrolled
 * voluntarily from {@code /account} already has a session and must not be given
 * a second one.
 *
 * @param backupCodes the plaintext codes, in existence for exactly this
 *                    response and never again
 * @param session     null when the caller already had a session
 */
record TotpEnableResult(List<String> backupCodes, TokenPair session) {
}
