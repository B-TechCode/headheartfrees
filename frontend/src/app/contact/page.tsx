import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/sections/PageHeader";
import { Callout } from "@/components/ui/Callout";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import {
  CONTACT_EMAIL,
  CONTACT_EMAIL_IS_PLACEHOLDER,
  CONTACT_MAILTO,
} from "@/lib/contact";
import { SectionRule } from "@/components/ui/SectionRule";

export const metadata: Metadata = {
  title: "Contact",
  description:
    "How to reach HeadHeartFreeS by email. This inbox is not a crisis line and is not monitored around the clock.",
};

/*
 * Contact.
 *
 * A mailto and an explanation, not a form: there is no endpoint to accept a
 * submission until Phase 7, and a form that silently drops what someone typed
 * would be worse than no form on this product in particular.
 *
 * The address lives in lib/contact.ts, and is now a real inbox belonging to
 * the project. The CONTACT_EMAIL_IS_PLACEHOLDER branch below is kept: if the
 * address ever goes dead again the page says so out loud rather than
 * presenting a dead address as a working one.
 *
 * The most important thing on this page is the line about what this inbox is
 * not. Someone in distress who emails and waits is worse off than someone who
 * was told plainly to call instead.
 */
const GOOD_REASONS = [
  "A helpline number on this site is wrong, out of date, or missing.",
  "You want feedback you left taken down.",
  "Something on the site is broken, or unusable with a screen reader or keyboard.",
  "You have a privacy question, or want a copy of what we hold.",
  "Press, partnerships, or you want to help pay for it.",
] as const;

export default function ContactPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        eyebrow="Contact"
        title="Getting in touch"
        lede="One inbox, read by a person, usually within a few days."
      />

      <Callout tone="important" title="This is not a crisis line" className="mt-10">
        <p>
          Email is not monitored around the clock and nobody is watching it overnight. If you
          need to talk to someone now, the helplines answer immediately and this inbox will
          not.
        </p>
        <p className="mt-4">
          <Link
            href="/crisis-resources"
            className={cn(
              "inline-flex min-h-11 items-center rounded-sm text-body font-medium",
              "text-(--color-text-accent) underline decoration-clay underline-offset-4",
              "hover:text-ink hover:decoration-ink-faint",
              focusRing,
            )}
          >
            See the helplines
          </Link>
        </p>
      </Callout>

      {CONTACT_EMAIL_IS_PLACEHOLDER ? (
        <Callout tone="caution" title="No address is connected yet" className="mt-8">
          <p>
            The address below is a placeholder and mail sent to it will not arrive. A real
            inbox is being set up. If you need something in the meantime, the helplines above
            are live and are the right route for anything urgent.
          </p>
        </Callout>
      ) : null}

      <section aria-labelledby="email-heading" className="mt-12">
        <SectionRule />

        <h2 id="email-heading" className="mt-4 font-display text-h3 text-ink">
          Email
        </h2>

        <p className="mt-4">
          <a
            href={CONTACT_MAILTO}
            className={cn(
              "inline-flex min-h-11 items-center rounded-sm font-display text-h3",
              "text-(--color-text-accent)",
              "underline decoration-ink-faint decoration-from-font underline-offset-[6px]",
              "transition-colors duration-150 ease-out hover:decoration-clay",
              CONTACT_EMAIL_IS_PLACEHOLDER && "line-through decoration-ink-faint",
              focusRing,
            )}
          >
            {CONTACT_EMAIL}
          </a>
        </p>

        <p className="mt-3 max-w-[68ch] text-body text-ink-soft">
          Say enough that we can act without writing back for details. If it is about
          feedback you want removed, quoting a line of it is the fastest way for us to find
          it.
        </p>
      </section>

      <section aria-labelledby="reasons-heading" className="mt-12">
        <SectionRule />

        <h2 id="reasons-heading" className="mt-4 font-display text-h3 text-ink">
          Good reasons to write
        </h2>

        <ul className="mt-4 flex max-w-[68ch] list-disc flex-col gap-2 pl-5 marker:text-clay">
          {GOOD_REASONS.map((reason) => (
            <li key={reason} className="pl-1 text-body text-ink-soft">
              {reason}
            </li>
          ))}
        </ul>

        <p className="mt-5 max-w-[68ch] text-body text-ink-soft">
          The first one matters more than the rest. A wrong crisis number is the worst thing
          that can be on this site, and we would rather hear about it from you than not hear
          about it at all.
        </p>
      </section>

      <section aria-labelledby="not-heading" className="mt-12 border-t border-rule pt-8">
        <SectionRule />

        <h2 id="not-heading" className="mt-4 font-display text-h3 text-ink">
          What this inbox cannot do
        </h2>

        <div className="mt-4 max-w-[68ch]">
          <p className="text-body text-ink-soft">
            It cannot give counselling, and nobody answering it is qualified to. It cannot
            retrieve something you wrote on the vent page, because no copy of it was ever
            made. And it cannot reach you quickly enough to help in an emergency.
          </p>
          <p className="mt-3 text-body text-ink-soft">
            For the first of those, and the last,{" "}
            <Link
              href="/crisis-resources"
              className={cn(
                "rounded-sm text-(--color-text-accent) underline decoration-ink-faint underline-offset-4",
                "hover:decoration-clay",
                focusRing,
              )}
            >
              the helplines
            </Link>{" "}
            are the answer.
          </p>
        </div>
      </section>
    </div>
  );
}
