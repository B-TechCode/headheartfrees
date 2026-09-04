/**
 * Shared class fragments for the UI primitives.
 *
 * These exist so that focus and disabled treatment are defined once. A design
 * system where each component invents its own disabled grey is a design system
 * that drifts within two phases.
 */

/**
 * Visible focus ring, keyboard-only.
 *
 * `focus-visible` rather than `focus` so a mouse click on a button does not
 * leave a ring behind it. `globals.css` also sets a `:focus-visible` outline on
 * every element as a backstop, so a primitive that forgets this constant still
 * cannot ship an unfocusable-looking control.
 */
export const focusRing =
  "outline-none focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus";

/**
 * Disabled treatment for controls that render as `<button>`, `<input>` or
 * `<textarea>`.
 *
 * Arbitrary values referencing the semantic tokens, rather than new Tailwind
 * theme keys: the palette already names these three roles, and adding
 * `--color-disabled-surface` to `@theme` would collide with the identically
 * named semantic variable — the self-reference bug documented in globals.css.
 */
export const disabledControl =
  "disabled:cursor-not-allowed " +
  "disabled:bg-(--color-disabled-surface) " +
  "disabled:text-(--color-disabled-text) " +
  "disabled:border-(--color-disabled-border) " +
  "disabled:shadow-none";

/**
 * Minimum interactive height. PROJECT_BRIEF.md §8 asks for tap targets of at
 * least 44px, which is also the WCAG 2.5.5 target size guidance.
 */
export const minTapTarget = "min-h-11";
