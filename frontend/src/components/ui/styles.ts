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
 * A secondary action: a real action, rendered as text rather than as a button.
 *
 * ===========================================================================
 * Why this is a constant and not five copies of the same class list
 * ===========================================================================
 *
 * "Back to home", "Why this exists", "Leave a note", "Never mind", "Try again",
 * "Support this space", "Go back to the vent". Seven places, one meaning: the
 * other thing you could do here. Before this constant existed they were seven
 * hand-copied class lists that had already drifted — one used a `rule-strong`
 * underline, one never changed colour on hover, one was a different size — and
 * a treatment that varies by page is not a treatment, it is a coincidence.
 *
 * This is the whole visual definition, focus ring included. Call sites add
 * layout and size only.
 *
 * ===========================================================================
 * The hierarchy this deliberately does NOT climb
 * ===========================================================================
 *
 * These do not become buttons. One filled button per section is what tells
 * someone which action is the main one; three equal buttons tell them nothing.
 * On `/vent/released` the point is sharper still — "Leave a note" sits directly
 * under the sentence "You don't have to — most people don't", and a filled
 * button there would contradict the line above it. The fix for "too quiet to
 * notice" is weight, not promotion.
 *
 * ===========================================================================
 * The three changes, and the numbers behind them
 * ===========================================================================
 *
 * Measured in a browser against every surface these actually land on, rather
 * than against the one the page nominally uses:
 *
 *   - `bone` #F7F4EF — /vent/released, /crisis-resources
 *   - `bone-raised` #FFFCF7 — the home hero, and inside a Callout on /voices
 *   - `bone-sunk` #EFEAE1 — the footer
 *
 * 1. **Text: `ink-soft` to `ink`.** 9.30:1 to 15.34:1 on bone, 8.52:1 to
 *    14.05:1 on the footer. `ink-soft` was never an accessibility failure; it
 *    was a *hierarchy* failure. Beside a filled accent button it read as caption
 *    text rather than as something you could press.
 *
 * 2. **Underline: `ink-faint`, at 2px instead of `auto`.** The brief offered
 *    `rule-strong` or `ink-faint`, whichever measured better. It is not close —
 *    `ink-faint` is 4.50:1 on bone against `rule-strong`'s 1.75:1, and the same
 *    ~2.6x gap holds on all three surfaces. `rule-strong` is under 3:1
 *    everywhere and cannot carry meaning on any of them; it is a divider
 *    colour. So the colour was already right in six of the seven places, which
 *    means the "hairline" was never really about colour: `underline` alone
 *    leaves `text-decoration-thickness: auto`, and the browser derives ~1px
 *    from the font. `decoration-2` is the change that makes it visible.
 *
 * 3. **Hover: a real colour shift.** Both the text and the underline move to
 *    `accent` (11.34:1 on bone), rather than the underline alone changing
 *    while the text stays put. Two of the seven did not shift colour at all.
 *
 * `underline-offset-4` keeps the thicker rule off the descenders.
 */
export const secondaryAction =
  "rounded-sm text-ink " +
  "underline decoration-ink-faint decoration-2 underline-offset-4 " +
  "transition-colors duration-150 ease-out " +
  "hover:text-(--color-text-accent) hover:decoration-(--color-text-accent) " +
  focusRing;

/**
 * Minimum interactive height. PROJECT_BRIEF.md §8 asks for tap targets of at
 * least 44px, which is also the WCAG 2.5.5 target size guidance.
 */
export const minTapTarget = "min-h-11";
