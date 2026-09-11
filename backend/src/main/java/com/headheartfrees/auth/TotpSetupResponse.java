package com.headheartfrees.auth;

/**
 * Everything the enrolment screen needs, and nothing that outlives it.
 *
 * <p>All three fields contain the same secret in different clothes. That is
 * unavoidable - an authenticator has to receive it somehow - and it is why this
 * response is issued only to a caller who has just proven a password, is never
 * cached, and is never written anywhere by the client.
 *
 * @param manualKey  base32, grouped in fours, for typing in by hand when a
 *                   camera is not available or the QR will not scan
 * @param otpauthUri the same secret as a {@code otpauth://totp/...} URI. Offered
 *                   as a link so that somebody enrolling on the phone that
 *                   holds the authenticator can tap through instead of
 *                   photographing their own screen.
 * @param qrDataUri  an SVG {@code data:} URI for an {@code <img>}. Rendered
 *                   server-side; see {@code TotpQrCode} for why.
 */
public record TotpSetupResponse(String manualKey, String otpauthUri, String qrDataUri) {

    static TotpSetupResponse from(TotpService.TotpSetup setup) {
        return new TotpSetupResponse(setup.manualKey(), setup.otpauthUri(), setup.qrDataUri());
    }
}
