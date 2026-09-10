import { describe, expect, it } from "vitest";
import { contentSecurityPolicy } from "./next.config";

/**
 * ===========================================================================
 * The failure this file exists to catch
 * ===========================================================================
 *
 * Phase 9 added a Content-Security-Policy in `headers()`. `headers()` applies
 * to `next dev` as well as to a build, and the dev server runs React Fast
 * Refresh, whose runtime calls `eval`. Without 'unsafe-eval' that call threw
 * inside a webpack module factory in `main-app.js`, which meant the client
 * bundle never finished executing and **nothing on the site hydrated**.
 *
 * The visible symptom was not a blank page — server HTML renders fine, so
 * every page looked correct. It was that /vent silently stopped working:
 * the textarea accepted typing the way any plain HTML textarea does, while
 * the React state behind it stayed empty, so the counter sat at `0 / 2000`
 * and "Release & Let Go" never enabled. It read exactly like a bug in
 * `VentComposer`, and `VentComposer` was never mounted.
 *
 * No component test could have caught it. jsdom does not enforce CSP, and
 * Testing Library mounts a component directly rather than serving it over
 * HTTP and hydrating it — so the entire defect lived in a layer the suite
 * did not reach. This file reaches it, by testing the policy itself.
 *
 * It is deliberately written in both directions. The dev half stops the
 * outage coming back; the production half stops the fix for it leaking into
 * a shipped policy, which is the more expensive of the two mistakes.
 */
describe("the content security policy", () => {
  const API = "https://api.example.com";

  /**
   * The production policy, verified in a real browser in phase 9 and pinned
   * here in full rather than by directive.
   *
   * Asserting the whole string is the point: a per-directive test would let
   * a development relaxation be appended to production without failing. If
   * this changes deliberately, the new policy must be checked against a real
   * browser on every route before this line is updated to match.
   */
  const PRODUCTION_POLICY =
    "default-src 'self'; " +
    "script-src 'self' 'unsafe-inline'; " +
    "style-src 'self'; " +
    "img-src 'self' data:; " +
    "font-src 'self'; " +
    `connect-src 'self' ${API}; ` +
    "frame-ancestors 'none'; " +
    "base-uri 'none'; " +
    "object-src 'none'; " +
    "form-action 'self'";

  describe("in production", () => {
    it("is exactly the policy that was verified in a browser", () => {
      expect(contentSecurityPolicy("production", API)).toBe(PRODUCTION_POLICY);
    });

    it("never permits eval", () => {
      // The single directive that matters most here: 'unsafe-eval' would give
      // an injected string the ability to become code, which is most of what
      // the rest of this header is written to prevent.
      expect(contentSecurityPolicy("production", API)).not.toContain("'unsafe-eval'");
    });

    it("keeps inline styles out", () => {
      const styleSrc = directive(contentSecurityPolicy("production", API), "style-src");
      expect(styleSrc).toBe("style-src 'self'");
    });
  });

  describe("in development", () => {
    const dev = contentSecurityPolicy("development", API);

    it("permits the eval that React Fast Refresh needs", () => {
      // Without this the dev server serves a site whose client bundle never
      // boots, and /vent looks broken in a way that points at the wrong file.
      expect(directive(dev, "script-src")).toContain("'unsafe-eval'");
    });

    it("permits the injected stylesheets and the HMR socket", () => {
      expect(directive(dev, "style-src")).toContain("'unsafe-inline'");
      expect(directive(dev, "connect-src")).toContain("ws:");
    });

    it("still names the API origin, and still refuses to be framed", () => {
      // The relaxations are additive and scoped. Everything the policy is
      // actually for stays on, so developing against it stays representative.
      expect(directive(dev, "connect-src")).toContain(API);
      expect(directive(dev, "frame-ancestors")).toBe("frame-ancestors 'none'");
      expect(directive(dev, "object-src")).toBe("object-src 'none'");
      expect(directive(dev, "base-uri")).toBe("base-uri 'none'");
    });
  });

  it("relaxes nothing outside development", () => {
    // Every directive in production is a subset of, or equal to, its dev
    // counterpart — and the two differ in exactly the three places above.
    const prod = contentSecurityPolicy("production", API);
    const dev = contentSecurityPolicy("development", API);

    const differing = names(prod).filter(
      (name) => directive(prod, name) !== directive(dev, name),
    );
    expect(differing.sort()).toEqual(["connect-src", "script-src", "style-src"]);
  });
});

/** One directive of a policy, whole, e.g. `script-src 'self' 'unsafe-inline'`. */
function directive(policy: string, name: string): string {
  const found = policy
    .split("; ")
    .find((part) => part === name || part.startsWith(`${name} `));
  if (!found) throw new Error(`No ${name} directive in: ${policy}`);
  return found;
}

/** Every directive name in a policy, in order. */
function names(policy: string): string[] {
  // `noUncheckedIndexedAccess` is on, and a directive with no name is a
  // malformed policy rather than something to paper over with a fallback.
  return policy.split("; ").map((part) => {
    const [name] = part.split(" ");
    if (!name) throw new Error(`Empty directive in: ${policy}`);
    return name;
  });
}
