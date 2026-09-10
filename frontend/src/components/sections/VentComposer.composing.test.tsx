import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { VentComposer } from "@/components/sections/VentComposer";
import { resetNavigation } from "@/test/navigation";

/**
 * ===========================================================================
 * What this file covers, and what it cannot
 * ===========================================================================
 *
 * The composer's whole contract with the person using it, before anything is
 * released: type, and the page acknowledges it. The counter moves and the
 * button becomes pressable. If either stops happening, /vent is a box that
 * takes words and refuses to do anything with them — which is worse than an
 * error, because it looks like being ignored.
 *
 * `VentComposer.session.test.tsx` already types into this box, but it types in
 * order to release, and asserts the button is enabled as a step on the way to
 * clicking it. The counter is asserted nowhere. This file makes the display
 * contract explicit rather than incidental, so it fails on its own terms.
 *
 * **It could not have caught the outage of 10 Sep 2026, and neither could any
 * test written like it.** That failure was a Content-Security-Policy that
 * blocked the `eval` React Fast Refresh needs, so the client bundle never
 * booted and no component on the site hydrated. The textarea kept typed text
 * the way plain HTML does while the state behind it stayed empty — these exact
 * symptoms, with `VentComposer` never mounted at all. Testing Library mounts
 * the component directly and jsdom does not enforce CSP, so the suite could
 * not see it from here. `next.config.test.ts` is the test that sees it, and
 * the two are complementary: this one holds the component honest, that one
 * holds the page it is delivered on honest.
 */
describe("writing in the vent composer", () => {
  beforeEach(() => {
    resetNavigation();
    // No fetch stub. Nothing here presses release, and a composer that reaches
    // the network merely because somebody typed would fail loudly on the
    // undefined global rather than passing quietly.
  });

  const box = () => screen.getByLabelText("What is weighing on you?");
  const releaseButton = () => screen.getByRole("button", { name: /release/i });

  /** The counter, found the way a screen reader finds it. */
  function counter(): HTMLElement {
    const describedBy = box().getAttribute("aria-describedby");
    expect(describedBy).toBeTruthy();
    const el = document.getElementById(describedBy as string);
    expect(el).not.toBeNull();
    return el as HTMLElement;
  }

  it("starts empty, with the button not yet pressable", () => {
    render(<VentComposer />);

    expect(box()).toHaveValue("");
    expect(counter()).toHaveTextContent("0 / 2000");
    expect(releaseButton()).toBeDisabled();
  });

  it("moves the counter and enables the button as soon as something is typed", async () => {
    const user = userEvent.setup();
    render(<VentComposer />);

    await user.type(box(), "today was long");

    // All three read from the same state, and all three are asserted: the
    // failure being guarded against is precisely the one where the DOM holds
    // the text but React does not.
    expect(box()).toHaveValue("today was long");
    expect(counter()).toHaveTextContent("14 / 2000");
    expect(releaseButton()).toBeEnabled();
  });

  it("keeps counting as the text grows", async () => {
    const user = userEvent.setup();
    render(<VentComposer />);

    await user.type(box(), "one");
    expect(counter()).toHaveTextContent("3 / 2000");

    await user.type(box(), " two");
    expect(counter()).toHaveTextContent("7 / 2000");

    // And back down. A counter derived from state falls when text is deleted;
    // one driven by a keystroke tally does not.
    await user.clear(box());
    expect(counter()).toHaveTextContent("0 / 2000");
    expect(releaseButton()).toBeDisabled();
  });

  it("does not offer release for whitespace alone", async () => {
    const user = userEvent.setup();
    render(<VentComposer />);

    await user.type(box(), "   ");

    // The counter reports what is in the box, because it is a character count
    // and three spaces are three characters. The button reads `text.trim()`,
    // so it stays put: there is nothing to let go of yet.
    expect(counter()).toHaveTextContent("3 / 2000");
    expect(releaseButton()).toBeDisabled();
  });

  it("counts what a prompt starter inserts, and enables release with it", async () => {
    const user = userEvent.setup();
    render(<VentComposer />);

    await user.click(screen.getByRole("button", { name: /I keep going over/i }));

    // "I keep going over" plus the trailing space the starter leaves for them
    // to carry on typing from.
    expect(box()).toHaveValue("I keep going over ");
    expect(counter()).toHaveTextContent("18 / 2000");
    expect(releaseButton()).toBeEnabled();
  });
});
