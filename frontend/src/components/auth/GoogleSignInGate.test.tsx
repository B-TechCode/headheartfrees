import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { noContent, stubFetch } from "@/test/http";
import { resetNavigation } from "@/test/navigation";

/**
 * The Google button is drawn when `NEXT_PUBLIC_GOOGLE_SIGN_IN` is `"true"`, and
 * not otherwise, on both forms.
 *
 * ===========================================================================
 * What this covers, and what covers the rest
 * ===========================================================================
 *
 * `/login` shipped with no Google button and no divider while the backend was
 * fully configured and a real sign-in had already gone through it. Nothing was
 * wrong with any component: the flag was absent from the operator's `.env`, so
 * compose's `:-false` default was inlined into the bundle.
 *
 * That defect had two halves and they need different tests.
 *
 *   - **Was the flag delivered?** A build-time question, and jsdom cannot ask
 *     it — under vitest `process.env.NEXT_PUBLIC_*` is an ordinary runtime
 *     lookup, which is exactly what makes `vi.stubEnv` work here and exactly
 *     why passing this proves nothing about a real bundle. That half is held
 *     by `EnvExampleComposeDriftTest` in the backend suite.
 *   - **Given the flag, does the button render?** This file.
 *
 * Both forms are checked because they carry the same gate written out twice,
 * and a change to one is not a change to the other.
 *
 * `resetModules` before each import is required, not tidiness:
 * `GOOGLE_SIGN_IN_ENABLED` is a module-level `const` evaluated once on first
 * import, so without it the first value read would decide every case below.
 */
describe("the Google sign-in gate", () => {
  beforeEach(() => {
    resetNavigation();
    // `LoginForm` sits under `SessionProvider`, which refreshes on mount. An
    // unstubbed fetch would reach the network from jsdom.
    stubFetch(() => noContent());
  });

  afterEach(() => {
    // `unstubGlobals` is on in vitest.config.mts; env stubs are a separate
    // opt-in and would otherwise leak into the next file.
    vi.unstubAllEnvs();
  });

  async function renderForm(form: "login" | "register", flag: string | undefined) {
    if (flag === undefined) {
      vi.stubEnv("NEXT_PUBLIC_GOOGLE_SIGN_IN", undefined as unknown as string);
    } else {
      vi.stubEnv("NEXT_PUBLIC_GOOGLE_SIGN_IN", flag);
    }
    vi.resetModules();

    // `SessionProvider` is imported here, after `resetModules`, and not at the
    // top of the file. `LoginForm` calls `useSession`, and the context object
    // identity has to match: a provider from the pre-reset module registry and
    // a consumer from the post-reset one are two different contexts, and the
    // consumer throws "must be used inside <SessionProvider>" while visibly
    // wrapped in one.
    const { SessionProvider } = await import("@/lib/auth/SessionProvider");

    if (form === "login") {
      const { LoginForm } = await import("@/components/auth/LoginForm");
      render(
        <SessionProvider>
          <LoginForm />
        </SessionProvider>,
      );
    } else {
      const { RegisterForm } = await import("@/components/auth/RegisterForm");
      render(
        <SessionProvider>
          <RegisterForm />
        </SessionProvider>,
      );
    }
  }

  const googleButton = () => screen.queryByRole("link", { name: /continue with google/i });

  describe.each(["login", "register"] as const)("on the %s form", (form) => {
    it("draws the button when the flag is true", async () => {
      await renderForm(form, "true");

      const button = googleButton();
      expect(button).toBeInTheDocument();

      // An anchor to the backend, not a fetch: the flow is a chain of top-level
      // redirects and an XHR cannot follow one. A refactor to `<button>` would
      // still satisfy "the button is present" while breaking sign-in, so the
      // href is asserted rather than the label alone.
      expect(button).toHaveAttribute("href", expect.stringContaining("/oauth2/authorization/google"));
    });

    it("draws nothing when the flag is false", async () => {
      await renderForm(form, "false");

      expect(googleButton()).not.toBeInTheDocument();
    });

    it("draws nothing when the flag is unset", async () => {
      await renderForm(form, undefined);

      expect(googleButton()).not.toBeInTheDocument();
    });

    // The comparison is `=== "true"`, so anything else is off. This pins that
    // an operator writing `NEXT_PUBLIC_GOOGLE_SIGN_IN=1` gets no button rather
    // than a half-working one, which is the safe direction: the backend, not
    // this flag, decides whether Google actually works.
    it("draws nothing for a truthy value that is not exactly \"true\"", async () => {
      await renderForm(form, "1");

      expect(googleButton()).not.toBeInTheDocument();
    });
  });
});
