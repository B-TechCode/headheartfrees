import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/sections/PageHeader";
import { HelplineList } from "@/components/sections/HelplineList";
import { Callout } from "@/components/ui/Callout";
import { INDIA_HELPLINES, INTERNATIONAL_HELPLINES } from "@/lib/helplines";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";

export const metadata: Metadata = {
  title: "Crisis resources",
  description:
    "Helplines in India and elsewhere, with the hours each one keeps. HeadHeartFreeS is not an emergency service and does not monitor what people write.",
};

/*
 * Crisis resources.
 *
 * Ordered by who answers, not by prose. The numbers are the first thing on the
 * page after one short line, because a person arriving here may not read past
 * the fold and should not have to.
 *
 * Every number is verified against the operator's own site and comes from
 * lib/helplines.ts, which is the single list the footer and the Phase 6 crisis
 * panel also read from.
 *
 * The "not an emergency service" statement sits below the numbers rather than
 * above them. It is necessary and it is honest, but leading with a disclaimer
 * makes someone in distress read a liability notice before they reach a phone
 * number.
 */
export default function CrisisResourcesPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        title="If you need to talk to someone now"
        lede="These are staffed by trained people, and they are free. Hours are listed on every line, because not all of them run overnight."
      />

      <section aria-labelledby="india-heading" className="mt-12">
        <h2 id="india-heading" className="font-display text-h3 text-ink">
          India
        </h2>
        <HelplineList helplines={INDIA_HELPLINES} />
      </section>

      <section aria-labelledby="international-heading" className="mt-12">
        <h2 id="international-heading" className="font-display text-h3 text-ink">
          Outside India
        </h2>
        <HelplineList helplines={INTERNATIONAL_HELPLINES} />

        <p className="mt-5 max-w-[68ch] text-body text-ink-soft">
          For anywhere else,{" "}
          <a
            href="https://findahelpline.com"
            rel="noopener noreferrer"
            target="_blank"
            className={cn(
              "rounded-sm text-(--color-text-accent) underline decoration-ink-faint underline-offset-4",
              "hover:decoration-clay",
              focusRing,
            )}
          >
            findahelpline.com
          </a>{" "}
          lists vetted services by country.
        </p>
      </section>

      <section aria-labelledby="emergency-heading" className="mt-14">
        <h2 id="emergency-heading" className="font-display text-h3 text-ink">
          When to call emergency services instead
        </h2>

        <div className="mt-4 max-w-[68ch]">
          <p className="text-body text-ink-soft">
            Helplines are for talking. They are not able to reach you physically. Call
            emergency services if someone has taken an overdose, is bleeding, has stopped
            responding, or is about to act on a plan to end their life.
          </p>
          <p className="mt-3 text-body text-ink-soft">
            In India that is <strong className="font-semibold text-ink">112</strong> for any
            emergency, or <strong className="font-semibold text-ink">102</strong> for an
            ambulance. Elsewhere, use your local emergency number.
          </p>
        </div>

        <ul className="mt-5 flex flex-col gap-3 sm:flex-row">
          <li>
            <a
              href="tel:112"
              className={cn(
                "inline-flex h-14 items-center justify-center rounded-md px-7",
                "font-sans text-body-lg font-medium",
                "border border-transparent bg-clay text-on-clay",
                "transition-colors duration-150 ease-out hover:bg-clay-hover active:bg-clay-active",
                focusRing,
              )}
            >
              Call 112 &middot; emergency
            </a>
          </li>
          <li>
            <a
              href="tel:102"
              className={cn(
                "inline-flex h-14 items-center justify-center rounded-md px-7",
                "font-sans text-body-lg font-medium",
                "border border-ink-faint bg-surface-raised text-ink",
                "transition-colors duration-150 ease-out hover:bg-surface-sunk",
                focusRing,
              )}
            >
              Call 102 &middot; ambulance
            </a>
          </li>
        </ul>
      </section>

      <Callout tone="important" title="What this site is not" className="mt-14">
        <p>
          HeadHeartFreeS is not an emergency service, not a counselling service, and not
          monitored. Nobody is reading what is typed into the vent page, so nobody there can
          see that you are struggling and reach out. That is deliberate, and it is the whole
          design, but it means this site cannot help in an emergency.
        </p>
        <p className="mt-3">
          If you need a person, use one of the numbers above. They are the point of this
          page.
        </p>
      </Callout>

      <p className="mt-10 text-body text-ink-soft">
        <Link
          href="/vent"
          className={cn(
            "rounded-sm underline decoration-ink-faint underline-offset-4",
            "hover:text-(--color-text-accent) hover:decoration-clay",
            focusRing,
          )}
        >
          Go back to the vent
        </Link>
      </p>

      <p className="mt-10 border-t border-rule pt-5 text-caption text-ink-soft">
        Numbers last checked against each operator&rsquo;s own website on 4 September 2026.
        If one of these is wrong or out of date, telling us matters more than most things you
        could email about.
      </p>
    </div>
  );
}
