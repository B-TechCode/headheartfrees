import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

/**
 * Typographic container for long-form copy: the privacy policy, the community
 * guidelines, the About text.
 *
 * Hand-rolled rather than `@tailwindcss/typography`, for the same reason
 * shadcn was skipped in Phase 2 — the plugin ships its own opinions about
 * scale, colour and rhythm, and un-picking them to match these tokens is more
 * work than the forty lines below. Everything here resolves to the Phase 2
 * scale, so policy pages and product pages set text identically.
 *
 * Measure is capped at 68 characters. Long legal copy at full container width
 * is the fastest way to make a page that nobody reads to the end of.
 */
export function Prose({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div
      className={cn(
        "max-w-[68ch]",
        // Headings
        "[&_h2]:font-display [&_h2]:text-h3 [&_h2]:text-ink [&_h2]:mt-12 [&_h2]:mb-3",
        "[&_h3]:font-display [&_h3]:text-h4 [&_h3]:text-ink [&_h3]:mt-8 [&_h3]:mb-2",
        // Body
        "[&_p]:text-body [&_p]:text-ink-soft [&_p]:mt-4",
        "[&_p:first-child]:mt-0",
        // Lists. Markers in clay: the one accent, at the smallest possible dose.
        "[&_ul]:mt-4 [&_ul]:flex [&_ul]:flex-col [&_ul]:gap-2 [&_ul]:pl-5",
        "[&_ul]:list-disc [&_ul]:marker:text-clay",
        "[&_ol]:mt-4 [&_ol]:flex [&_ol]:flex-col [&_ol]:gap-2 [&_ol]:pl-5",
        "[&_ol]:list-decimal [&_ol]:marker:text-ink-faint",
        "[&_li]:text-body [&_li]:text-ink-soft [&_li]:pl-1",
        // Inline
        "[&_strong]:font-semibold [&_strong]:text-ink",
        "[&_a]:text-(--color-text-accent) [&_a]:underline [&_a]:decoration-ink-faint",
        "[&_a]:underline-offset-4 hover:[&_a]:decoration-clay",
        // Separators between major blocks, not between every paragraph.
        "[&_hr]:my-10 [&_hr]:border-rule",
        className,
      )}
    >
      {children}
    </div>
  );
}
