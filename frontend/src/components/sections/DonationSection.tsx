import Link from "next/link";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { SectionRule } from "@/components/ui/SectionRule";

/**
 * The donation section on /about (PROJECT_BRIEF.md §7).
 *
 * Quiet by instruction and by judgement: a heading, two sentences, one link.
 * No banner, no urgency, no progress bar, no suggested amounts, no total
 * raised. A product whose pitch is "nothing is asked of you" cannot then ask
 * loudly.
 *
 * **This block deliberately does not list what support pays for.** It used to,
 * and `/support` said the same thing in slightly different words — two copies
 * of one claim, which is how they drift until a visitor finds the site
 * contradicting itself about money. `/support` is the source of truth for the
 * detail; this says only enough to explain the link. Anything more specific
 * belongs there.
 */
export function DonationSection() {
  return (
    <section aria-labelledby="support-heading" className="mt-16 border-t border-rule pt-10">
      <SectionRule />

      <h2 id="support-heading" className="mt-4 font-display text-h3 text-ink">
        Supporting this
      </h2>

      <div className="mt-3 max-w-[68ch]">
        <p className="text-body text-ink-soft">
          This costs a small amount to keep running, and anyone who wants to put something
          towards that can.
        </p>
        <p className="mt-3 text-body text-ink-soft">
          Nothing here is behind a payment, and nothing will be. Giving changes nothing about
          what you get, which is the point of mentioning it once and then leaving it alone.
        </p>
      </div>

      <Link
        href="/support"
        className={cn(
          "mt-5 inline-flex min-h-11 items-center rounded-sm text-body",
          "text-(--color-text-accent) underline decoration-ink-faint underline-offset-4",
          "transition-colors duration-150 ease-out hover:decoration-accent",
          focusRing,
        )}
      >
        How to support this space
      </Link>
    </section>
  );
}
