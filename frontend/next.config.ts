import type { NextConfig } from "next";

/**
 * The API origin the browser is allowed to talk to.
 *
 * Read here at BUILD time, the same value the client bundle is compiled
 * against. If these two ever disagree the app makes requests the browser then
 * refuses, so they are deliberately taken from one variable.
 */
const API_ORIGIN = (() => {
  const raw = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  try {
    return new URL(raw).origin;
  } catch {
    // A malformed value would otherwise produce a CSP that silently blocks
    // every API call. Failing the build is the louder, cheaper outcome.
    throw new Error(`NEXT_PUBLIC_API_BASE_URL is not a valid URL: ${raw}`);
  }
})();

/**
 * Content-Security-Policy, built from what this app actually loads.
 *
 * ===========================================================================
 * Why script-src carries 'unsafe-inline', and when to change it
 * ===========================================================================
 *
 * The App Router emits ~13 inline <script> blocks per page carrying the RSC
 * flight payload (`self.__next_f.push`). They are not optional and their
 * content changes per page and per build, so hashes are impractical.
 *
 * The stronger policy is a per-request nonce with 'strict-dynamic', set from
 * middleware. **It was considered and deliberately not taken.** See PHASE_LOG
 * phase 9 §1 for the full argument and the conditions that should reverse it.
 * The short version: a nonce forces Next to opt every page out of static
 * generation, and this site is almost entirely static; the XSS path a nonce
 * defends does not currently exist here, because nothing renders rich or
 * third-party content.
 *
 * **Revisit this the moment that stops being true** — a rich-text field, an
 * embed, a third-party script, or any HTML rendered from user input.
 *
 * ===========================================================================
 * The rest, and why each value is what it is
 * ===========================================================================
 *
 * - `style-src 'self'` with NO 'unsafe-inline'. Verified: zero <style> tags,
 *   zero style= attributes and zero style={{}} in source across every route.
 *   Most Next apps cannot say this; it is worth not giving up.
 * - `img-src 'self' data:` — the paper-grain overlay in globals.css is an
 *   inline SVG data: URL, and a CSS background-image is governed by img-src.
 * - `font-src 'self'` — next/font self-hosts; nothing is fetched from Google.
 * - `connect-src` names the API origin explicitly. The frontend and backend are
 *   different origins, so omitting this blocks every fetch — and it fails
 *   silently from the user's side, which is why it is called out here.
 * - `frame-ancestors 'none'` — this site must not be embeddable. A clickjacked
 *   vent box is a way to capture what somebody types.
 * - `base-uri 'none'` stops an injected <base> retargeting relative URLs;
 *   `object-src 'none'` removes the plugin surface; `form-action 'self'` keeps
 *   a form from posting off-site. The app uses fetch, not form posts, and the
 *   Google button is a link rather than a form, so none of these bind.
 */
const CSP = [
  "default-src 'self'",
  "script-src 'self' 'unsafe-inline'",
  "style-src 'self'",
  "img-src 'self' data:",
  "font-src 'self'",
  `connect-src 'self' ${API_ORIGIN}`,
  "frame-ancestors 'none'",
  "base-uri 'none'",
  "object-src 'none'",
  "form-action 'self'",
].join("; ");

/**
 * Report-only mode, for verifying a policy before enforcing it.
 *
 * Set `CSP_REPORT_ONLY=true` at build time to send the policy as
 * `Content-Security-Policy-Report-Only`, which logs violations to the console
 * without blocking anything. Used to collect real violations from a real
 * browser across every route before this policy was turned on.
 */
const CSP_REPORT_ONLY = process.env.CSP_REPORT_ONLY === "true";

/**
 * HSTS, off by default and off in every local run.
 *
 * **The primary place for this header is the TLS terminator**, not the
 * application — whatever proxy or load balancer holds the certificate. This
 * flag exists so a deployment without one is not left without the header.
 *
 * It is BUILD time, not runtime: `headers()` is resolved when Next builds, so
 * changing this needs a rebuild. That is stated in HANDOVER.
 *
 * **Enabling HSTS is close to irreversible for the length of max-age.** A
 * browser that has seen it will refuse plain HTTP to this host for a year, so
 * it goes on last, after TLS is confirmed working. See HANDOVER 3.x.
 */
const ENABLE_HSTS = process.env.ENABLE_HSTS === "true";

/**
 * Deny everything this app does not use.
 *
 * The app has no camera, microphone, geolocation, payment or sensor code, so
 * every one of these is a capability it can give up for nothing. `browsing
 * -topics` is included to opt out of Topics-style interest inference, which is
 * not something a mental-health site should participate in by default.
 */
const PERMISSIONS_POLICY = [
  "accelerometer=()",
  "autoplay=()",
  "camera=()",
  "browsing-topics=()",
  "display-capture=()",
  "encrypted-media=()",
  "fullscreen=()",
  "geolocation=()",
  "gyroscope=()",
  "magnetometer=()",
  "microphone=()",
  "midi=()",
  "payment=()",
  "usb=()",
].join(", ");

const securityHeaders = [
  {
    key: CSP_REPORT_ONLY ? "Content-Security-Policy-Report-Only" : "Content-Security-Policy",
    value: CSP,
  },
  { key: "X-Content-Type-Options", value: "nosniff" },
  // Redundant with frame-ancestors on current browsers, kept for older ones.
  { key: "X-Frame-Options", value: "DENY" },
  /*
   * no-referrer, not the browser default.
   *
   * This site loads no third-party resources and has exactly one outbound
   * link — findahelpline.com on /crisis-resources. That single link is the
   * argument: `strict-origin-when-cross-origin` would still tell that site
   * the visitor came from here, and the pages someone reads on this domain
   * are precisely the ones that should not appear in anybody else's logs.
   * Nothing here needs a referrer, so nothing is lost by sending none.
   */
  { key: "Referrer-Policy", value: "no-referrer" },
  { key: "Permissions-Policy", value: PERMISSIONS_POLICY },
  ...(ENABLE_HSTS
    ? [
        {
          key: "Strict-Transport-Security",
          value: "max-age=31536000; includeSubDomains",
        },
      ]
    : []),
];

const nextConfig: NextConfig = {
  // Emits a minimal server bundle for the runtime Docker stage.
  output: "standalone",
  reactStrictMode: true,
  poweredByHeader: false,
  typescript: {
    // Never ship a build that does not typecheck.
    ignoreBuildErrors: false,
  },
  eslint: {
    ignoreDuringBuilds: false,
  },
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
};

export default nextConfig;
