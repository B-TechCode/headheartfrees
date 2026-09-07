import { vi } from "vitest";

/**
 * A stand-in for `next/navigation`.
 *
 * There is no App Router outside `next dev` / `next build`, so any component
 * calling `useRouter`, `usePathname` or `useSearchParams` throws in jsdom
 * unless the module is replaced. Every test file that renders one of those
 * components does:
 *
 * ```ts
 * vi.mock("next/navigation", async () =>
 *   (await import("@/test/navigation")).navigationModule());
 * ```
 *
 * The dynamic import is not decoration: `vi.mock` factories are hoisted above
 * the imports in the file, so they cannot close over anything declared there.
 */
export const routerPush = vi.fn();
export const routerReplace = vi.fn();

/** Mutable so a test can say which page it is on before rendering. */
export const location = { pathname: "/", search: "" };

export function navigationModule() {
  return {
    useRouter: () => ({
      push: routerPush,
      replace: routerReplace,
      back: vi.fn(),
      forward: vi.fn(),
      refresh: vi.fn(),
      prefetch: vi.fn(),
    }),
    usePathname: () => location.pathname,
    useSearchParams: () => new URLSearchParams(location.search),
  };
}

/** Call in `beforeEach`: `clearMocks` empties the calls but not this. */
export function resetNavigation() {
  location.pathname = "/";
  location.search = "";
}
