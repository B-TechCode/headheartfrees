import { fileURLToPath } from "node:url";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

/**
 * Vitest, jsdom, Testing Library. No Playwright.
 *
 * These tests exist to hold one property that no amount of care in review
 * catches reliably: **the session must never gate the page**. `/vent` is the
 * product, rule 2.2 says it needs no account, and the way that breaks is not a
 * deliberate gate — it is a provider somewhere up the tree that returns null
 * for 300ms while a refresh is in flight, on a slow connection, for someone who
 * will simply close the tab. `VentComposer.session.test.tsx` leaves a refresh
 * deliberately unresolved and then types and releases through it.
 *
 * A browser-driving suite would test more and would also need a running
 * backend, a database and a browser download in CI, which is a large amount of
 * machinery for a phase whose new logic is all in one provider and three forms.
 * jsdom cannot tell us how this looks; it can tell us what it does.
 *
 * `globals` stays off deliberately. Importing `describe`/`it`/`expect` costs a
 * line per file and keeps `tsconfig.json` free of an ambient types entry that
 * would apply to the application code as well as the tests.
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    // Mirrors the `@/*` path in tsconfig.json. Kept by hand rather than via
    // vite-tsconfig-paths: one alias is not worth a dependency, and the two
    // are next to each other in the same directory.
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    clearMocks: true,
    restoreMocks: true,
    unstubGlobals: true,
  },
});
