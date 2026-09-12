/**
 * The accent hairline that announces a section.
 *
 * A 1px accent rule, 40px wide, sitting above a heading. The motif was already
 * in the site — the hero, the crisis strip, the crisis panel and the footer's
 * Support column each drew their own `<span>` — and it is now the site-wide
 * signal that a section starts here rather than the page merely continuing.
 *
 * `bg-accent` is 10.38:1 on `surface-sunk` and 11.34:1 on `surface`. Both
 * clear the 3:1 that SC 1.4.11 asks of a non-text graphic, with wide margin
 * since the maroon retone.
 *
 * `aria-hidden` because it carries no information a screen reader needs: the
 * heading it sits above already says the section has started, and a decorative
 * rule announced as anything would be noise.
 *
 * ===========================================================================
 * Why some inline spans survive
 * ===========================================================================
 *
 * `CrisisStrip`, `CrisisPanel`, `/vent`, `/vent/released` and `/auth/callback`
 * still draw the span by hand. Those files were deliberately not opened during
 * this pass — the crisis surfaces are not to be restyled, and the vent pages
 * stay as quiet as they are. The markup they emit is identical to this
 * component's, so nothing looks different; there is simply no reason to touch
 * a file to produce the same bytes.
 */
export function SectionRule() {
  return <span aria-hidden="true" className="block h-px w-10 bg-accent" />;
}
