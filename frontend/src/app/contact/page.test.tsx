import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import ContactPage from "./page";
import {
  CONTACT_EMAIL,
  CONTACT_EMAIL_IS_PLACEHOLDER,
  CONTACT_MAILTO,
} from "@/lib/contact";

/**
 * The contact page, now that the address is real.
 *
 * Two properties, and the second is the one that matters.
 *
 * **1. The placeholder notice is actually gone.** `CONTACT_EMAIL` and
 * `CONTACT_EMAIL_IS_PLACEHOLDER` have to move together, and they are two
 * separate lines in one file. A real address with the flag still `true` strikes
 * the address through and tells visitors not to write to an inbox that works.
 *
 * **2. The page still says what this inbox is not.** This is the important one.
 * While the address was a placeholder nobody could email a void; now they can,
 * and someone in distress who writes and waits is worse off than someone who
 * was told plainly to call instead. So the "not a crisis line" callout, the
 * "not monitored around the clock" line, and the link to `/crisis-resources`
 * are pinned here rather than left to review. If a future edit tidies any of
 * them away, this fails and names which.
 */
describe("the contact page", () => {
  it("shows no placeholder notice, because the address is real", () => {
    render(<ContactPage />);

    expect(CONTACT_EMAIL_IS_PLACEHOLDER).toBe(false);
    expect(screen.queryByText(/No address is connected yet/i)).toBeNull();
    expect(screen.queryByText(/will not arrive/i)).toBeNull();
  });

  it("links the real address as a mailto, not struck through", () => {
    render(<ContactPage />);

    const link = screen.getByRole("link", { name: CONTACT_EMAIL });

    expect(link.getAttribute("href")).toBe(CONTACT_MAILTO);
    expect(link.getAttribute("href")).toBe(
      "mailto:headheartfrees@gmail.com?subject=HeadHeartFreeS%20enquiry",
    );
    expect(link.className).not.toMatch(/line-through/);
  });

  it("still states plainly that this is not a crisis line", () => {
    render(<ContactPage />);

    expect(screen.getByText(/This is not a crisis line/i)).toBeTruthy();
    expect(screen.getByText(/not monitored around the clock/i)).toBeTruthy();
    expect(screen.getByText(/nobody is watching it overnight/i)).toBeTruthy();
  });

  it("still routes anyone who needs someone now to the helplines", () => {
    render(<ContactPage />);

    const toCrisisResources = screen
      .getAllByRole("link")
      .filter((a) => a.getAttribute("href") === "/crisis-resources");

    expect(toCrisisResources.length).toBeGreaterThan(0);
  });
});
