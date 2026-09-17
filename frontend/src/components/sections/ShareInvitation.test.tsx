import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ShareInvitation } from "@/components/sections/ShareInvitation";
import { SHARE_FORM_IS_PLACEHOLDER, SHARE_FORM_URL } from "@/lib/share";

/**
 * The claims that must be on screen before the link is.
 *
 * This is a copy test, which is unusual, and it is deliberate. Everywhere else
 * on this site the copy is free to change and a test asserting on sentences
 * would be an obstacle. Here the sentences *are* the feature: someone arriving
 * at `/vent` has been told repeatedly that nothing is stored and nobody reads
 * it, and this block links to a form that stores everything and is read by a
 * person. Remove one of these statements — in a tidy-up, a tone pass, a
 * shortening — and the site has misled somebody at the one point where its
 * credibility lives. Nothing else in the codebase would notice.
 *
 * So the assertions are on meaning rather than on wording: each one matches a
 * phrase that cannot be removed without removing the claim, and any rewrite
 * that still makes the claim can be made to pass by updating the regex. A
 * rewrite that drops the claim cannot.
 *
 * Geometry is not tested — jsdom has no layout, so the 44px target and the
 * separation from "Release & Let Go" were checked in a browser and recorded in
 * `PHASE_LOG.md`.
 */
const CLAIMS: ReadonlyArray<readonly [string, RegExp]> = [
  ["it is sent and it is stored", /is sent,? and it is stored|it is sent, it is stored/i],
  ["a person reads it", /a person reads it/i],
  ["it goes to Google rather than to this site", /to Google[^.]*rather than to this site/i],
  ["Google's terms apply there", /Google’s terms apply/i],
  ["it may ask where to reach you", /where to reach you/i],
  ["you can leave that blank", /leave (both|that) blank/i],
  ["no Google account is needed", /(do not need a Google account|No Google account needed)/i],
];

describe.each(["vent", "released"] as const)("the share block on /%s", (variant) => {
  it("states every difference from the vent box before the link", () => {
    const { container } = render(<ShareInvitation variant={variant} />);
    const text = container.textContent ?? "";

    for (const [claim, pattern] of CLAIMS) {
      expect(pattern.test(text), `missing claim: ${claim}`).toBe(true);
    }
  });

  it("does not repeat the form's own confidentiality guarantee", () => {
    const { container } = render(<ShareInvitation variant={variant} />);

    /*
     * The form's description promises "100% confidential". This site cannot
     * vouch for data in someone else's Google account, so it does not say so.
     * See the header comment in ShareInvitation.tsx.
     */
    expect(container.textContent).not.toMatch(/100%|confidential|private and secure/i);
  });

  it("opens a new tab safely and names the destination in the link text", () => {
    render(<ShareInvitation variant={variant} />);

    const anchor = screen.getByRole("link", { name: /Google Forms/i });

    expect(anchor.getAttribute("href")).toBe(SHARE_FORM_URL);
    expect(anchor.getAttribute("target")).toBe("_blank");
    expect(anchor.getAttribute("rel")).toBe("noopener noreferrer");

    // The warning is in the link's own accessible name, not only in a title.
    expect(anchor).toHaveAccessibleName(/opens in a new tab/i);
  });

  it("puts the link under a heading of its own, not beside the release button", () => {
    render(<ShareInvitation variant={variant} />);

    const section = screen.getByRole("region");
    const heading = within(section).getByRole("heading");

    // "If you would rather be heard" / "If you wanted someone to read it".
    expect(heading.textContent).toMatch(/heard|read/i);
    expect(within(section).getByRole("link", { name: /Google Forms/i })).toBeInTheDocument();
    // A secondary action. If this ever renders as a button the hierarchy is wrong.
    expect(within(section).queryByRole("button")).toBeNull();
  });
});

describe("the released variant", () => {
  it("says the words are already gone, so nobody expects to find them in the form", () => {
    const { container } = render(<ShareInvitation variant="released" />);

    expect(container.textContent).toMatch(/What you wrote is gone/i);
    expect(container.textContent).toMatch(/starting again/i);
  });
});

describe("lib/share.ts", () => {
  /*
   * Not a behaviour test. `SHARE_FORM_IS_PLACEHOLDER` renders both blocks away
   * entirely, which is correct when there is no form to send anyone to and
   * wrong the rest of the time — a flag left `true` after a real URL is
   * supplied is a silent removal of the feature, and the only thing that would
   * catch it is this line failing.
   */
  it("is not left flagged as a placeholder while a real URL is configured", () => {
    expect(SHARE_FORM_URL).toMatch(/^https:\/\/docs\.google\.com\/forms\//);
    expect(SHARE_FORM_IS_PLACEHOLDER).toBe(false);
  });
});
