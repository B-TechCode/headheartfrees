import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/sections/PageHeader";
import { DonationSection } from "@/components/sections/DonationSection";
import { Prose } from "@/components/ui/Prose";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { SectionRule } from "@/components/ui/SectionRule";

export const metadata: Metadata = {
  title: "About",
  description:
    "Why HeadHeartFreeS exists, what it refuses to do, and how it is paid for. Not therapy, and not a substitute for it.",
};

/*
 * About: mission, principles, donation section (PROJECT_BRIEF.md §7).
 *
 * Two things this copy is careful about. It does not claim clinical outcomes —
 * no "feel better", no "reduce anxiety", no implied treatment effect — because
 * there is no evidence for any of that and this is a mental-health adjacent
 * product where an unsupported claim does real harm. And it does not inflate
 * the problem to justify itself; stigma and cost are stated plainly and left
 * to stand on their own.
 */

const PRINCIPLES = [
  {
    title: "What you write is yours",
    body: "The vent text never reaches a server. That is not a policy we could change quietly one day; there is no endpoint that accepts it, and the release page has nothing to send.",
  },
  {
    title: "Nothing is asked of you first",
    body: "No account, no email, no cookie banner negotiation before you can type. The page opens and the box is there.",
  },
  {
    title: "No advice",
    body: "Nothing here analyses your words or replies to them. There is a real difference between being helped and being heard, and this site only claims the second one — and only in the sense that you got to say it.",
  },
  {
    title: "Not a substitute for help",
    body: "Writing something down is not treatment. When what you need is a person, the helplines are one click away on every page of this site.",
  },
] as const;

export default function AboutPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        eyebrow="About"
        title="Why this exists"
        lede="Sometimes a person does not need to be fixed. They need to put something down."
      />

      <Prose className="mt-10">
        <p>
          Talking about a bad week is still difficult for a lot of people. Saying it to family
          invites worry, saying it to friends invites advice, and saying it to a professional
          costs money that most people do not have spare. So it gets carried instead.
        </p>
        <p>
          Therapy is the right answer for a great many of those people, and this site is not
          an alternative to it. But therapy has a waiting list and a price, and the thing you
          are carrying is here now, at 2am, on a Tuesday.
        </p>
        <p>
          This is a smaller idea than therapy. You write down what is sitting on you, you
          press a button, and the words are gone. Nothing is stored, nothing is analysed, and
          nothing answers back. It is the digital version of saying something out loud in an
          empty room, which people have been doing for a long time because it sometimes
          helps.
        </p>
        <p>
          We are not going to tell you it will make you feel better. It might do nothing. It
          costs you nothing to find out, and it asks nothing of you in return.
        </p>
      </Prose>

      <section aria-labelledby="principles-heading" className="mt-16 border-t border-rule pt-10">
        <SectionRule />

        <h2 id="principles-heading" className="mt-4 font-display text-h2 text-ink">
          What this holds to
        </h2>

        <dl className="mt-8 grid gap-x-12 gap-y-8 md:grid-cols-2">
          {PRINCIPLES.map((principle) => (
            <div key={principle.title}>
              <dt className="font-display text-h4 text-ink">{principle.title}</dt>
              <dd className="mt-2 text-body text-ink-soft">{principle.body}</dd>
            </div>
          ))}
        </dl>
      </section>

      <section aria-labelledby="honest-heading" className="mt-16 border-t border-rule pt-10">
        <SectionRule />

        <h2 id="honest-heading" className="mt-4 font-display text-h3 text-ink">
          What we cannot tell you yet
        </h2>
        <div className="mt-3 max-w-[68ch]">
          <p className="text-body text-ink-soft">
            This is new, and it has no users to speak of. You will not find visitor counts,
            testimonials or satisfaction figures on this site, because there are none that
            would be true. When there is a real number worth showing, it will be the count of
            releases, and it will be the actual one.
          </p>
        </div>
      </section>

      <DonationSection />

      <p className="mt-14 text-body text-ink-soft">
        <Link
          href="/vent"
          className={cn(
            "rounded-sm underline decoration-accent underline-offset-4",
            "hover:text-(--color-text-accent)",
            focusRing,
          )}
        >
          Go to the vent
        </Link>
      </p>
    </div>
  );
}
