package com.headheartfrees.auth;

import io.nayuki.qrcodegen.QrCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Renders an {@code otpauth://} URI to an SVG {@code data:} URI for the
 * enrolment screen.
 *
 * <h2>Why an SVG data URI and not a PNG, an endpoint, or a client-side render</h2>
 *
 * <p><strong>Not a PNG</strong>: producing one means {@code BufferedImage} and
 * {@code ImageIO}, which drags AWT into a headless container for a picture made
 * entirely of squares. The SVG is a few hundred bytes of text and scales to
 * whatever the layout wants.
 *
 * <p><strong>Not a {@code GET /totp/qr.svg} endpoint</strong>: that would put a
 * URL carrying a live credential into browser history and any proxy log between
 * here and the person. The data URI exists only inside a JSON response to an
 * authenticated POST and is gone when the tab closes.
 *
 * <p><strong>Not rendered in the browser</strong>: the frontend has zero
 * runtime dependencies today, which is worth keeping, and the production CSP is
 * {@code style-src 'self'} with no {@code 'unsafe-inline'} - verified in phase
 * 9 and pinned by {@code next.config.test.ts}. A client-side QR component is
 * one inline style away from breaking that. An {@code <img>} with a
 * {@code data:} source is already covered by the existing
 * {@code img-src 'self' data:}, so this needs no policy change at all.
 *
 * <p>Rendering into {@code <img>} rather than inlining the SVG markup also
 * means the document never contains the QR as live DOM: an {@code <img>} is an
 * isolated, script-free context, and there is no {@code dangerouslySetInnerHTML}
 * anywhere in this feature.
 *
 * <h2>The single path</h2>
 *
 * One {@code <path>} of black modules over a white rectangle, rather than one
 * {@code <rect>} per module. A version-2 code is 25x25; per-module rectangles
 * would be several hundred elements and about ten times the bytes, and the
 * whole thing has to fit inside a JSON field.
 */
final class TotpQrCode {

    /**
     * The white margin around the code, in modules. Four is the quiet zone the
     * QR specification requires - scanners lose the finder patterns without it,
     * and a code that "sometimes does not scan" is an awful thing to debug on
     * someone else's phone.
     */
    private static final int QUIET_ZONE = 4;

    private TotpQrCode() {
    }

    /**
     * @return {@code data:image/svg+xml;base64,...}, ready for an {@code <img>}
     *         {@code src}
     */
    static String svgDataUri(String content) {
        // MEDIUM recovery: the default for this kind of code, and enough to
        // survive a phone camera at an angle without inflating the version.
        QrCode qr = QrCode.encodeText(content, QrCode.Ecc.MEDIUM);
        String svg = toSvg(qr);
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private static String toSvg(QrCode qr) {
        int side = qr.size + QUIET_ZONE * 2;

        StringBuilder path = new StringBuilder();
        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                if (qr.getModule(x, y)) {
                    if (!path.isEmpty()) {
                        path.append(' ');
                    }
                    path.append('M').append(x + QUIET_ZONE).append(',').append(y + QUIET_ZONE)
                            .append("h1v1h-1z");
                }
            }
        }

        // Explicit #ffffff and #000000 rather than currentColor or a CSS
        // variable. This is scanned by a camera, not read by a person: the
        // contrast has to be maximal regardless of the page's theme, and an
        // <img> cannot inherit the document's colours anyway.
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" "
                + "viewBox=\"0 0 " + side + " " + side + "\" "
                + "shape-rendering=\"crispEdges\">"
                + "<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>"
                + "<path d=\"" + path + "\" fill=\"#000000\"/>"
                + "</svg>";
    }
}
