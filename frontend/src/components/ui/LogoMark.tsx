import { cn } from "@/lib/cn";

/**
 * The full exhale logo — head, heart and wing — inside a circle.
 *
 * This is NOT a replacement for `Logo`. It is a second mark for the two places
 * that can give it room. The distinction is load-bearing:
 *
 * - `Logo` is drawn geometry that inherits `currentColor`, so it works on ink,
 *   on accent and on bone from one file, and it holds together at 16px. It
 *   stays in the navbar and stays the favicon.
 * - This one is an auto-trace of the supplied JPEG: 1,100 straight line
 *   segments, no curves, and the maroon-to-gold gradient posterised into eight
 *   flat bands. It is multicolour and fixed, so it cannot inherit anything.
 *
 * **It was measured at navbar size and it does not work there.** At 24-40px the
 * face profile disappears entirely — the nose, lip and chin gaps fill in, and
 * ink coverage climbs from 17.6% at 160px to 34.4% at 24px, which is what
 * filling-in looks like when you count it. Mean ink lands at 1.64:1 against
 * bone at 24px: a pale smudge, not a logo. Hence the two fixed sizes below,
 * both well clear of it.
 *
 * The circle is a framing choice, requested separately. It does not improve the
 * mark's legibility and was never expected to — it gives the artwork a defined
 * edge and a consistent footprint. If a properly drawn vector ever replaces the
 * trace, the circle can be kept or dropped on its own merits.
 *
 * ---
 *
 * **No inline styles, and not as a matter of taste.** next.config.ts ships
 * `style-src 'self'` with no 'unsafe-inline' in production, and documents zero
 * `style=` attributes across every route as a property worth defending. A
 * `style={{ width }}` here is silently dropped by the browser in production —
 * the attribute is in the HTML, `el.style.width` reads empty, and the element
 * shrink-wraps to its content. Development relaxes the directive, so it looks
 * correct there and is wrong once deployed. The size is therefore a variant
 * with literal class names Tailwind can see at build time, not a number.
 */

/** Intrinsic size of the cleaned artwork, from its viewBox. Wider than tall. */
const ART_W = 291;
const ART_H = 222;

/**
 * The mark occupies ~0.66 of the circle's diameter.
 *
 * The artwork is contained, never cropped — a crop at this aspect would clip
 * the wing tip, which is the one part of the drawing with any energy in it. At
 * 0.66 the mark's half-diagonal is 0.427x the diameter against the circle's
 * 0.5, so it sits inside the edge with roughly 7% breathing room all round.
 *
 * Widths below are that fraction resolved to whole pixels; heights follow from
 * the artwork's aspect. They are passed as `width`/`height` ATTRIBUTES, which
 * are presentational markup rather than CSS — unaffected by style-src, and
 * they reserve the box before the file arrives, so nothing reflows.
 */
const SIZES = {
  /** 128px — pages where the mark is supporting, e.g. above a sign-in form. */
  md: { circle: "h-32 w-32", markW: 84, markH: Math.round((84 * ART_H) / ART_W) },
  /** 160px — pages that can give it the room, e.g. the top of /about. */
  lg: { circle: "h-40 w-40", markW: 106, markH: Math.round((106 * ART_H) / ART_W) },
} as const;

type LogoMarkProps = {
  /** Fixed sizes only. Both are comfortably above where the trace breaks down. */
  size?: keyof typeof SIZES;
  className?: string;
  /**
   * Accessible name. Defaults to `null` — presentational — because this mark
   * is ornament beside a page heading that already names the place. Pass a
   * string only if it is ever the sole identification of the brand on a view.
   */
  title?: string | null;
};

export function LogoMark({ size = "lg", className, title = null }: LogoMarkProps) {
  const { circle, markW, markH } = SIZES[size];
  const decorative = title === null;

  return (
    <span
      className={cn(
        "inline-flex shrink-0 items-center justify-center rounded-full",
        // The fill cannot define the circle: every bone-ish tone measures
        // 1.00-1.23:1 against the surfaces this sits on. `surface-raised` is
        // chosen because it is closest to the artwork's own ground and gives
        // the dark structural bands their best contrast (12.95:1). The border
        // is what makes the circle a circle — see band-rule in globals.css,
        // whose documented purpose is exactly this kind of decorative edge.
        "border border-band-rule bg-surface-raised",
        circle,
        className,
      )}
      {...(decorative
        ? { "aria-hidden": true }
        : { role: "img", "aria-label": title })}
    >
      {/*
        Referenced, not inlined. The file is 4.6KB of path data and cannot use
        currentColor, so inlining it would cost every page that imports this
        component and buy nothing. `img-src 'self'` already covers it.

        A plain <img>, and next/image is the wrong tool here for the same reason
        it is in TotpSetup: the optimiser does not process SVG, so it would add
        a wrapper and a loader to hand back the identical bytes. The width and
        height attributes reserve the box, which is the only thing next/image
        would otherwise have bought.
      */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src="/logo-mark.svg" alt="" width={markW} height={markH} decoding="async" />
    </span>
  );
}
