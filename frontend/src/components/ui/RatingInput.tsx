"use client";

import { useId } from "react";
import { cn } from "@/lib/cn";

/**
 * A 1–5 rating, built on native radio inputs.
 *
 * ===========================================================================
 * Why radios and not buttons
 * ===========================================================================
 *
 * The usual star rating is a row of `<button>`s with an image inside and no
 * accessible name. It is unusable with a screen reader, it has five tab stops
 * where it should have one, and it announces nothing about which of them is
 * chosen. It is the single most common accessibility failure in this widget,
 * and it is what §8's brief about not looking generated is aimed at too.
 *
 * A native radio group already is the composite widget this needs. The browser
 * gives, for free and correctly in every assistive technology:
 *
 * - **one tab stop** for the whole group, not five
 * - **arrow keys** to move and select within it
 * - **`aria-checked` state** announced without a single ARIA attribute
 * - the group's name read from the `<legend>` on focus
 *
 * Writing this with `role="radiogroup"` and `tabIndex` juggling would
 * reimplement all of that, slightly wrong. There is no JavaScript in this
 * component's keyboard path at all.
 *
 * A `<select>` fallback was considered and rejected: a second control is a
 * second thing to get wrong, and a radiogroup is already the standard answer.
 *
 * ===========================================================================
 * How it is announced
 * ===========================================================================
 *
 * The `<legend>` carries the question; each input carries only its position.
 * A screen reader user hears roughly:
 *
 *   "How was this place for you? — 3 of 5, radio button, 3 of 5, not checked."
 *
 * The labels are deliberately *not* sentiment words. "1 — disappointing" or
 * "5 — life-changing" would put words in someone's mouth about a place they
 * came to because they were having a hard day.
 *
 * Nothing is preselected. A default rating is a rating the person did not
 * choose, and this one is required.
 */
export function RatingInput({
  value,
  onChange,
  name = "rating",
  legend = "How was this place for you?",
  error,
  required = true,
}: {
  value: number | null;
  onChange: (rating: number) => void;
  name?: string;
  legend?: string;
  error?: string;
  required?: boolean;
}) {
  const errorId = useId();

  return (
    <fieldset className="min-w-0 border-0 p-0">
      <legend className="font-sans text-body-sm font-medium text-ink">
        {legend}
      </legend>

      <div
        className="mt-3 flex items-center gap-1"
        aria-describedby={error ? errorId : undefined}
      >
        {[1, 2, 3, 4, 5].map((star) => (
          <label
            key={star}
            className={cn(
              "group relative inline-flex h-11 w-11 cursor-pointer items-center justify-center",
              "rounded-md transition-colors duration-150 ease-out",
              "hover:bg-accent-wash",
            )}
          >
            {/*
              `sr-only`, not `hidden` and not `opacity-0` with a negative
              z-index: the input has to stay focusable and hit-testable, or the
              keyboard behaviour this component exists for goes away. `peer`
              lets the visible star react to its checked and focus state
              without JavaScript.
            */}
            <input
              type="radio"
              name={name}
              value={star}
              checked={value === star}
              required={required}
              onChange={() => onChange(star)}
              className="peer sr-only"
            />
            <span className="sr-only">{star} of 5</span>
            <Star
              filled={value !== null && star <= value}
              className={cn(
                "h-7 w-7 transition-colors duration-150 ease-out",
                // The focus ring goes on the star, because the input it
                // belongs to is visually hidden. Without this the group is
                // operable by keyboard and gives no sign of where focus is.
                "peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2",
                "peer-focus-visible:outline-(--color-focus)",
                "rounded-sm",
              )}
            />
          </label>
        ))}
      </div>

      {error ? (
        <p id={errorId} className="mt-2 font-sans text-body-sm text-danger">
          {error}
        </p>
      ) : null}
    </fieldset>
  );
}

/**
 * The star.
 *
 * Filled and empty differ by **weight and fill, not only colour** — an outline
 * against a solid shape — so the chosen rating is legible without relying on
 * hue. WCAG 1.4.1: colour must not be the only visual means of conveying
 * information, and a row of stars distinguished purely by a colour change is
 * the textbook failure of it.
 *
 * A custom path rather than an emoji: §8 rules emoji out as icons, and ⭐
 * renders as a different picture on every platform.
 */
function Star({ filled, className }: { filled: boolean; className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      aria-hidden="true"
      focusable="false"
      className={cn(className, filled ? "text-accent" : "text-ink-faint")}
      fill={filled ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth={filled ? 1 : 1.5}
      strokeLinejoin="round"
    >
      <path d="M12 3.6l2.6 5.5 5.9.8-4.3 4.2 1.1 6-5.3-2.9-5.3 2.9 1.1-6L3.5 9.9l5.9-.8z" />
    </svg>
  );
}
