import { beforeEach, describe, expect, it } from "vitest";
import {
  RETURN_TO_COOKIE,
  rememberReturnTo,
  sanitiseReturnTo,
  takeReturnTo,
} from "@/lib/auth/return-to";
import { signInHref } from "@/lib/auth/sign-in-href";

/**
 * The open-redirect guard.
 *
 * This is the piece of the phase most likely to be quietly relaxed later —
 * "starts with a slash" looks like a complete check and is not. A return-to
 * value that a browser resolves to another host turns a link beginning with
 * this site's own domain into a phishing hop, and the fact that the URL looks
 * right is the entire attack.
 */
describe("sanitiseReturnTo", () => {
  it("accepts ordinary same-origin paths", () => {
    expect(sanitiseReturnTo("/about")).toBe("/about");
    expect(sanitiseReturnTo("/vent")).toBe("/vent");
    expect(sanitiseReturnTo("/account?tab=you")).toBe("/account?tab=you");
    expect(sanitiseReturnTo("/crisis-resources#india")).toBe("/crisis-resources#india");
  });

  it("refuses anything that could leave this origin", () => {
    // Protocol-relative: a browser reads these as another host, despite the
    // leading slash that a naive check would accept.
    expect(sanitiseReturnTo("//evil.example")).toBeNull();
    expect(sanitiseReturnTo("//evil.example/login")).toBeNull();
    expect(sanitiseReturnTo("/\\evil.example")).toBeNull();

    expect(sanitiseReturnTo("https://evil.example")).toBeNull();
    expect(sanitiseReturnTo("javascript:alert(1)")).toBeNull();
    expect(sanitiseReturnTo("about:blank")).toBeNull();
    expect(sanitiseReturnTo("vent")).toBeNull();
  });

  it("refuses control characters", () => {
    // A newline in a destination is a header-splitting primitive anywhere this
    // value is echoed, and there is no legitimate path containing one.
    expect(sanitiseReturnTo("/about\nSet-Cookie: x=1")).toBeNull();
    expect(sanitiseReturnTo("/about\r\n/evil")).toBeNull();
  });

  it("refuses the pages it makes no sense to return to", () => {
    // Returning to /login after signing in is a loop, and /auth/callback does
    // nothing without a fresh redirect from Google behind it.
    expect(sanitiseReturnTo("/login")).toBeNull();
    expect(sanitiseReturnTo("/login?next=/about")).toBeNull();
    expect(sanitiseReturnTo("/register")).toBeNull();
    expect(sanitiseReturnTo("/auth/callback")).toBeNull();
  });

  it("refuses nothing, empty, and the absurdly long", () => {
    expect(sanitiseReturnTo(null)).toBeNull();
    expect(sanitiseReturnTo(undefined)).toBeNull();
    expect(sanitiseReturnTo("")).toBeNull();
    expect(sanitiseReturnTo(`/${"a".repeat(600)}`)).toBeNull();
  });
});

describe("the return-to cookie", () => {
  beforeEach(() => {
    document.cookie = `${RETURN_TO_COOKIE}=; Max-Age=0; Path=/`;
  });

  it("is read once and then gone", () => {
    rememberReturnTo("/account");

    expect(takeReturnTo()).toBe("/account");
    // A destination that survived would apply to the next sign-in too, which
    // is a stale intention arriving at a moment nobody asked for it.
    expect(takeReturnTo()).toBeNull();
  });

  it("does not store a destination it would refuse to use", () => {
    rememberReturnTo("//evil.example");
    expect(document.cookie).not.toContain("evil.example");
    expect(takeReturnTo()).toBeNull();
  });

  it("re-checks on the way out, not only on the way in", () => {
    // The cookie is writable by script, so what comes back is not necessarily
    // what went in. Validating only on write puts the guard on the wrong side
    // of the trust boundary.
    document.cookie = `${RETURN_TO_COOKIE}=${encodeURIComponent("//evil.example")}; Path=/`;
    expect(takeReturnTo()).toBeNull();
  });
});

describe("signInHref", () => {
  it("carries the current page", () => {
    expect(signInHref("/about")).toBe("/login?next=%2Fabout");
  });

  it("omits the parameter when it would say nothing", () => {
    expect(signInHref("/")).toBe("/login");
    expect(signInHref(null)).toBe("/login");
    // Already on a sign-in page: the sanitiser refuses it, so no loop.
    expect(signInHref("/login")).toBe("/login");
  });
});
