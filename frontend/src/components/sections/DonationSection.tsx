import Link from "next/link";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";

/**
 * The donation section on /about (PROJECT_BRIEF.md §7).
 *
 * Quiet by instruction and by judgement: a heading, three sentences on what
 * support actually pays for, one link. No banner, no urgency, no progress bar,
 * no suggested amounts, no total raised. A product whose pitch is "nothing is
 * asked of you" cannot then ask loudly.
 *
 * The costs named below are the real ones for this stack — a server, a domain,
 * a database. No figure is given, because there isn't a verified one to give.
 */
export function DonationSection() {
  return (
    <section aria-labelledby="support-heading" className="mt-16 border-t border-rule pt-10">
      <h2 id="support-heading" className="font-display text-h3 text-ink">
        Supporting this
      </h2>

      <div className="mt-3 max-w-[68ch]">
        <p className="text-body text-ink-soft">
          This runs on a small server, a domain, and a database that holds no vent text. That
          is most of what support pays for.
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
          "transition-colors duration-150 ease-out hover:decoration-clay",
          focusRing,
        )}
      >
        How to support this space
      </Link>
    </section>
  );
}
