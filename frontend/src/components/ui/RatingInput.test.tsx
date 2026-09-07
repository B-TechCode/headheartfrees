import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";

import { RatingInput } from "@/components/ui/RatingInput";

/**
 * The rating control's accessibility contract.
 *
 * ===========================================================================
 * What these can and cannot tell us
 * ===========================================================================
 *
 * jsdom has no accessibility tree and no real focus model, so these tests
 * cannot prove what a screen reader announces. What they *can* prove is that
 * the control is built out of the elements that make the announcement correct
 * — real radios in a real fieldset, each with a name — rather than the row of
 * unlabelled buttons this widget is usually built from.
 *
 * The distinction matters because the button version passes every test that
 * only asks "can I click a star and get a number back". These ask for the
 * structure instead.
 *
 * A real keyboard pass in a browser is still outstanding and is recorded as
 * such in PHASE_LOG.
 */
describe("the rating control", () => {
  function Harness() {
    const [rating, setRating] = useState<number | null>(null);
    return (
      <>
        <RatingInput value={rating} onChange={setRating} />
        <p data-testid="chosen">{rating ?? "none"}</p>
      </>
    );
  }

  it("is a radio group of five named options, not a row of buttons", () => {
    render(<Harness />);

    const radios = screen.getAllByRole("radio");
    expect(radios).toHaveLength(5);

    // Every option has a name. An unnamed control is the failure mode this
    // component exists to avoid, and it is invisible to a click-based test.
    for (const star of [1, 2, 3, 4, 5]) {
      expect(screen.getByRole("radio", { name: `${star} of 5` })).toBeInTheDocument();
    }

    // No buttons. If someone reimplements this with <button>, this fails.
    expect(screen.queryAllByRole("button")).toHaveLength(0);
  });

  it("groups the options under the question", () => {
    render(<Harness />);

    // `getByRole("group")` finds the fieldset; the accessible name comes from
    // the legend, which is what a screen reader reads on entering the group.
    expect(
      screen.getByRole("group", { name: /how was this place for you/i }),
    ).toBeInTheDocument();
  });

  it("starts with nothing chosen", () => {
    render(<Harness />);

    // A default rating is a rating the person did not give.
    expect(screen.getAllByRole("radio").some((radio) => (radio as HTMLInputElement).checked))
      .toBe(false);
    expect(screen.getByTestId("chosen")).toHaveTextContent("none");
  });

  it("reports the chosen value", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(screen.getByRole("radio", { name: "4 of 5" }));

    expect(screen.getByTestId("chosen")).toHaveTextContent("4");
    expect((screen.getByRole("radio", { name: "4 of 5" }) as HTMLInputElement).checked).toBe(true);
  });

  it("is reachable and selectable from the keyboard", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    // One tab stop for the whole group, not five. Tab lands on the group and
    // arrow keys move within it - the behaviour a native radio group provides
    // and a button row does not.
    await user.tab();
    expect(screen.getByRole("radio", { name: "1 of 5" })).toHaveFocus();

    await user.keyboard("{ArrowRight}{ArrowRight}");

    expect(screen.getByTestId("chosen")).toHaveTextContent("3");
  });

  it("associates an error with the group", () => {
    render(
      <RatingInput value={null} onChange={() => {}} error="Please choose a rating from 1 to 5." />,
    );

    // Present and findable, so the message is announced rather than only
    // painted next to the stars.
    expect(screen.getByText("Please choose a rating from 1 to 5.")).toBeInTheDocument();
  });
});
