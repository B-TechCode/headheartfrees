import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/sections/PageHeader";
import { Callout } from "@/components/ui/Callout";
import { Prose } from "@/components/ui/Prose";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { CONTACT_MAILTO } from "@/lib/contact";

export const metadata: Metadata = {
  title: "Community guidelines",
  description:
    "What gets published on the feedback wall and what does not. Venting is private and unmoderated, because nothing you write there is ever stored.",
};

/*
 * Community guidelines.
 *
 * The unusual thing about this page is its scope, so the scope is the first
 * thing on it. These rules cannot govern venting: there is nothing to moderate,
 * because nothing is transmitted or kept. They govern the feedback wall only,
 * which is the sole place where one person's words are shown to another.
 *
 * Saying that out loud does double duty — it sets the boundary, and it is
 * itself the clearest possible statement of the privacy model.
 */
export default function CommunityGuidelinesPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        eyebrow="Community"
        title="Guidelines for the feedback wall"
        lede="These rules cover one part of the site: the public feedback people choose to leave. They do not cover venting, and they could not."
      />

      <Callout tone="important" title="Venting is not moderated, and cannot be" className="mt-10">
        <p>
          Nothing you write on the vent page is transmitted, stored or seen. There is no copy
          of it for anyone to review, report or remove. That means there are no rules about
          what you may write there, and no way for us to enforce any if there were.
        </p>
        <p className="mt-3">
          Write whatever you need to. It is not going anywhere.
        </p>
      </Callout>

      <Prose className="mt-12">
        <h2>What the feedback wall is</h2>
        <p>
          After releasing, you can leave a short piece of feedback with a rating. If it is
          approved it appears publicly on the site, with the name and location you chose to
          give, or without them if you gave none. This is the only place on
          HeadHeartFreeS where something you wrote is shown to other people, and it only
          happens because you deliberately submitted it.
        </p>

        <h2>Everything is reviewed before it appears</h2>
        <p>
          Nothing publishes automatically. Every submission sits in a queue until a person
          reads it and approves it. This is slower than publishing straight away, and it is
          the right trade for a site people arrive at while struggling.
        </p>
        <p>
          Being held is not a judgement, and most things are approved. If something is
          rejected it simply does not appear; you will not receive a notification, because we
          have no address to send one to unless you have an account.
        </p>

        <h2>What gets published</h2>
        <ul>
          <li>How the site felt to use, including when it did not help.</li>
          <li>What you would change, add or remove.</li>
          <li>Something short about your experience, if you want to leave one.</li>
          <li>Criticism. Unflattering feedback is published like anything else.</li>
        </ul>

        <h2>What gets rejected</h2>
        <ul>
          <li>Anything identifying another person: names, workplaces, handles, photographs.</li>
          <li>Abuse, harassment, or attacks on a group of people.</li>
          <li>
            Detailed description of methods of self-harm or suicide. Talking about the fact
            that you are struggling is fine. Specifics are not published, because other people
            read this wall.
          </li>
          <li>Advertising, links to products, and search-engine spam.</li>
          <li>Anything that looks like a person in immediate danger. That gets a helpline in reply, not a public post.</li>
          <li>Contact details, yours or anyone else&rsquo;s, including phone numbers and emails.</li>
        </ul>

        <h2>Asking for something to be removed</h2>
        <p>
          You can have your feedback taken down at any time and you do not have to give a
          reason. Email us, quote enough of the wording that we can find the right entry, and
          it will be removed.
        </p>
        <p>
          If you left feedback while signed in, mention the address on the account, which
          makes it quicker. If you left it anonymously we may not be able to confirm it was
          yours, in which case we will still take it down if the request is plausible.
          Removing something that should have stayed up is a much smaller harm than leaving up
          something someone wants gone.
        </p>

        <h2>Reporting something you have seen</h2>
        <p>
          If something on the wall breaks these rules and is live, tell us and it comes down
          while we look at it.
        </p>
      </Prose>

      <p className="mt-10">
        <a
          href={CONTACT_MAILTO}
          className={cn(
            "inline-flex min-h-11 items-center rounded-sm text-body",
            "text-(--color-text-accent) underline decoration-ink-faint underline-offset-4",
            "hover:decoration-accent",
            focusRing,
          )}
        >
          Email about a removal or a report
        </a>
      </p>

      <p className="mt-8 border-t border-rule pt-5 text-body-sm text-ink-soft">
        See also the{" "}
        <Link
          href="/privacy"
          className={cn(
            "rounded-sm underline decoration-ink-faint underline-offset-4 hover:decoration-accent",
            focusRing,
          )}
        >
          privacy policy
        </Link>{" "}
        for what is stored, and{" "}
        <Link
          href="/crisis-resources"
          className={cn(
            "rounded-sm underline decoration-ink-faint underline-offset-4 hover:decoration-accent",
            focusRing,
          )}
        >
          crisis resources
        </Link>{" "}
        if you need a person rather than a page.
      </p>
    </div>
  );
}
