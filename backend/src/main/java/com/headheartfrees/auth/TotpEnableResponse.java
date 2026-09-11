package com.headheartfrees.auth;

import java.util.List;

/**
 * The backup codes, and a session when the caller did not already have one.
 *
 * <h2>This is the only time the codes exist outside the person's hands</h2>
 *
 * They are hashed with Argon2id on the way into the database and there is no
 * endpoint that returns them again. If this response is lost - a closed tab, a
 * dropped connection after the write committed - the codes are gone, and the
 * only way back to ten is to regenerate them with a current code. The screen
 * that renders this has to say so before the person navigates away.
 *
 * @param backupCodes ten single-use codes, shown once
 * @param session     present only when enrolment was reached on an enrolment
 *                    ticket - the admin who was stopped at sign-in. Absent for
 *                    somebody who enrolled from an account page they were
 *                    already signed in to.
 */
public record TotpEnableResponse(List<String> backupCodes, AccessTokenResponse session) {
}
