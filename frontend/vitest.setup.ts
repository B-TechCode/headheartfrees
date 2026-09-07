import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

afterEach(() => {
  cleanup();

  // jsdom keeps one `document` for the whole file, so cookies written by one
  // test are still there in the next one. The session hint decides whether a
  // page load makes a network call at all, so a leaked cookie does not fail a
  // test — it quietly changes what the test is testing.
  for (const entry of document.cookie.split("; ")) {
    const name = entry.split("=")[0];
    if (name) {
      document.cookie = `${name}=; Max-Age=0; Path=/`;
    }
  }
});
