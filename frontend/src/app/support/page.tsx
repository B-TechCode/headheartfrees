import type { Metadata } from "next";
import Link from "next/link";
import { Callout } from "@/components/ui/Callout";
import { PageHeader } from "@/components/sections/PageHeader";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import {
  SUPPORT_PAYMENT_IS_PLACEHOLDER,
  SUPPORT_UPI_ID,
  SUPPORT_UPI_LINK,
} from "@/lib/support";
import { SectionRule } from "@/components/ui/SectionRule";

export const metadata: Metadata = {
  title: "Support this space",
  description:
    "What it costs to run HeadHeartFreeS, and how to help with that if you want to. The site is free and stays free either way.",
};

/*
 * /support
 *
 * ===========================================================================
 * The page this must not become
 * ===========================================================================
 *
 * Someone arriving here has usually just used the site to put something heavy
 * down. Asking them for money at that moment is only defensible if the ask is
 * genuinely easy to walk past, so the walk-away sits in the same sentence as
 * the ask rather than in a footnote under it.
 *
 * Deliberately absent, and none of these may be added later:
 *
 * - a progress bar, a goal, a total raised, a donor count
 * - urgency or scarcity of any kind: no "we need help to survive", no
 *   "running costs are due", no deadline
 * - suggested amounts, and no `am=` parameter on the UPI link, which would be
 *   a default figure presented as a choice
 * - tiers, rewards, memberships, badges, early access
 * - guilt in any register, including the gentle ones — "if you can spare it",
 *   "every little helps", "only if it has been useful to you". Each of those
 *   makes not giving into a small failure, which is the thing this page is
 *   most likely to get subtly wrong.
 *
 * ===========================================================================
 * No numbers
 * ===========================================================================
 *
 * The costs below are named as categories and never as figures. There is no
 * verified monthly total for this project, and an invented one would break the
 * promise the opening paragraph makes two sentences earlier. **If you are
 * adding a number here, it needs to come from a real invoice.**
 */
export default function SupportPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        eyebrow="Support"
        title="Supporting this space"
        lede="This site costs a small amount each month to run: a server, a domain, and a database that holds no vent text. If you want to put something towards that, you can. If you would rather not, nothing changes — the site is free, it stays free, and there is no better version of it behind a payment."
      />

      <section aria-labelledby="what-heading" className="mt-14">
        <SectionRule />

        <h2 id="what-heading" className="mt-4 font-display text-h3 text-ink">
          What it pays for
        </h2>

        <ul className="mt-4 flex max-w-[62ch] flex-col gap-3 text-body text-ink-soft">
          <li>
            <span className="text-ink">A server.</span> One small machine, running the site and
            the API.
          </li>
          <li>
            <span className="text-ink">A domain.</span> The address you typed to get here.
          </li>
          <li>
            <span className="text-ink">A database.</span> It holds accounts and the notes people
            chose to publish. It has never held anything written in the vent box, and there is
            no table it could go in.
          </li>
        </ul>

        <p className="mt-5 max-w-[62ch] text-body text-ink-soft">
          That is the whole list. No figure is given here because there is not a verified one to
          give, and a number invented to look concrete would be worth less than saying so.
        </p>
      </section>

      <section aria-labelledby="not-heading" className="mt-12">
        <SectionRule />

        <h2 id="not-heading" className="mt-4 font-display text-h3 text-ink">
          What it does not buy
        </h2>

        <p className="mt-4 max-w-[62ch] text-body text-ink-soft">
          Nothing. There is no tier, no membership, no badge, and no part of this site that
          opens up because someone gave something. Everything here is already available to
          everyone, including the people who will never see this page.
        </p>
      </section>

      <section aria-labelledby="how-heading" className="mt-12">
        <SectionRule />

        <h2 id="how-heading" className="mt-4 font-display text-h3 text-ink">
          How to give
        </h2>

        {SUPPORT_PAYMENT_IS_PLACEHOLDER ? (
          /*
           * Written for a visitor, not for a developer. "Payment not
           * configured" is a status line about the software; someone who came
           * here meaning to give needs to know that they cannot, that they have
           * not done anything wrong, and that nothing is owed. No apology and
           * no "coming soon" — one is theatre and the other is a promise about
           * a date nobody has set.
           */
          <Callout tone="caution" title="There is no way to give anything yet" className="mt-4">
            <p>
              No payment method has been set up, so nothing can be sent to this site at the
              moment — and that is fine. Everything here works and stays free regardless. If you
              came to this page meaning to help, the thought is the part that was going to be
              worth anything anyway.
            </p>
          </Callout>
        ) : (
          <div className="mt-4">
            <p className="max-w-[62ch] text-body text-ink-soft">
              Any UPI app, to the ID below. Whatever amount you decide on — there is no
              suggested figure and no minimum.
            </p>

            <p className="mt-5 font-sans text-body-lg text-ink">
              <span className="sr-only">UPI ID: </span>
              <code className="rounded-sm bg-surface-sunk px-2 py-1">{SUPPORT_UPI_ID}</code>
            </p>

            {/*
              A plain link, not a button styled as a call to action. On a phone
              this opens the payment app; on a desktop it does nothing useful,
              which is why the ID above is the primary thing and this is the
              convenience.
            */}
            <a
              href={SUPPORT_UPI_LINK}
              className={cn(
                "mt-5 inline-flex min-h-11 items-center rounded-sm text-body",
                "text-(--color-text-accent) underline decoration-ink-faint underline-offset-4",
                "transition-colors duration-150 ease-out hover:decoration-clay",
                focusRing,
              )}
            >
              Open this in a UPI app
            </a>
          </div>
        )}
      </section>

      <section className="mt-14 border-t border-rule pt-8">
        <p className="max-w-[62ch] text-body text-ink-soft">
          If you came here from the box and you are not sure why, you can just go back. That is
          the part of this that matters.
        </p>

        <Link
          href="/vent"
          className={cn(
            "mt-4 inline-flex min-h-11 items-center rounded-sm text-body-lg",
            "text-(--color-text-accent) underline decoration-clay underline-offset-4",
            "transition-colors duration-150 ease-out hover:text-ink hover:decoration-ink-faint",
            focusRing,
          )}
        >
          Back to the vent box
        </Link>
      </section>
    </div>
  );
}
