import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Footer } from "@/components/layout/Footer";
import { SOCIAL_LINKS } from "@/lib/social";

/**
 * The social row's three properties that regress silently.
 *
 * Geometry is not tested here — jsdom has no layout, so the 44px targets and
 * the stacking behaviour were measured in a real browser instead and are
 * recorded in `PHASE_LOG.md`. What jsdom *can* hold is the part a reviewer
 * cannot see by looking at the rendered page:
 *
 * **1. Every link has a real accessible name.** The glyphs are `aria-hidden`,
 * so the anchor's `aria-label` is the only name these links have. Drop it and
 * a screen reader announces four consecutive "link"s in the footer of every
 * page, which is close to the worst available outcome. The name has to be the
 * platform's own — "LinkedIn", never "social link".
 *
 * **2. `rel="noopener noreferrer"` with `target="_blank"`.** These are the only
 * links on the site that open a new tab, and `noopener` is what stops the
 * opened page reaching back through `window.opener`. It is one attribute, it
 * is invisible when it is missing, and nothing else in this codebase needs it,
 * so nothing else would catch its removal.
 *
 * **3. The row renders from `lib/social.ts` and nothing is hardcoded.** That
 * file is where the handover replacement happens (HANDOVER.md §2.9). A URL
 * pasted directly into the JSX would survive editing that file, which is the
 * specific way the developer's personal accounts end up outliving the
 * developer.
 */
describe("the footer's social row", () => {
  it("names every link after the platform it goes to", () => {
    render(<Footer />);

    for (const link of SOCIAL_LINKS) {
      const anchor = screen.getByRole("link", { name: link.label });
      expect(anchor.getAttribute("href")).toBe(link.href);
    }

    expect(SOCIAL_LINKS.map((l) => l.label)).toEqual([
      "LinkedIn",
      "Facebook",
      "GitHub",
      "Portfolio",
    ]);
  });

  it("opens them in a new tab without handing over window.opener", () => {
    render(<Footer />);

    for (const link of SOCIAL_LINKS) {
      const anchor = screen.getByRole("link", { name: link.label });

      expect(anchor.getAttribute("target")).toBe("_blank");
      expect(anchor.getAttribute("rel")).toBe("noopener noreferrer");
      expect(anchor.querySelector("svg")?.getAttribute("aria-hidden")).toBe("true");
    }
  });
});
