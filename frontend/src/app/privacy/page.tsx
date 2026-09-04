import type { Metadata } from "next";
import Link from "next/link";
import { PageHeader } from "@/components/sections/PageHeader";
import { Callout } from "@/components/ui/Callout";
import { Prose } from "@/components/ui/Prose";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { CONTACT_MAILTO } from "@/lib/contact";

export const metadata: Metadata = {
  title: "Privacy policy",
  description:
    "What HeadHeartFreeS stores and what it does not. Vent text is never transmitted. The release endpoint records an optional mood label and a timestamp, and nothing else.",
};

/*
 * Privacy policy.
 *
 * Written against the code as it stands today, not from a template. Every claim
 * here is checkable: the vent domain has no DTO with a text field, the release
 * endpoint takes { mood } only, and there is no auth system yet.
 *
 * Two deliberate choices:
 *
 * - The legal-review notice is at the top, not buried at the bottom. It is
 *   accurate but it is not a lawyer's document, and a reader deserves to know
 *   that before they rely on it.
 * - It says what will change in Phase 4 rather than pre-writing the policy for
 *   a system that does not exist. A privacy policy describing accounts nobody
 *   can create would be false in the other direction.
 */
export default function PrivacyPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        eyebrow="Privacy"
        title="What is stored, and what never is"
        lede="Short version: the thing you write on the vent page is never sent anywhere. Everything else on this page is detail."
      />

      <Callout tone="caution" title="This needs a lawyer before launch" className="mt-10">
        <p>
          This policy is accurate about how the software behaves — it was written from the
          code, not from a template. It has not been reviewed by a lawyer, and it does not yet
          address the formal requirements of the DPDP Act, the GDPR, or any other regime we
          may fall under. Treat it as a truthful description of the system, not as a
          compliant legal document. It must be reviewed before this site is public.
        </p>
      </Callout>

      <Prose className="mt-12">
        <h2>Vent text is never transmitted</h2>
        <p>
          What you type into the vent page stays in your browser&rsquo;s memory. It is not
          sent to our servers, not written to a log, and not stored and later deleted. When
          you press release, it is cleared from the page and it is gone.
        </p>
        <p>
          This is enforced by the shape of the software rather than by our restraint. There is
          no field on any endpoint that accepts vent content. Even if we wanted the text, the
          system as built has nowhere to put it.
        </p>

        <h2>What the release button does send</h2>
        <p>
          Pressing release makes one request to our server. It contains:
        </p>
        <ul>
          <li>The mood label you picked, if you picked one. If you did not, this is empty.</li>
          <li>Nothing else.</li>
        </ul>
        <p>
          The server records that, plus the time it arrived. It does not record your IP
          address, a user ID, a device identifier, or a session. The purpose is a count of how
          many times the button has been pressed.
        </p>

        <h2>Feedback is stored, because you chose to publish it</h2>
        <p>
          Feedback is different from venting in kind, not just in degree. You write it
          deliberately, knowing it may appear publicly. So it is stored: the rating, the
          message, and the display name and location if you supplied them.
        </p>
        <p>
          It is reviewed before it appears and you can have it removed at any time. See the{" "}
          <Link href="/community-guidelines">community guidelines</Link>.
        </p>

        <h2>Accounts do not exist yet</h2>
        <p>
          <strong>
            There is currently no way to create an account on this site, so no account data
            exists.
          </strong>{" "}
          The Sign in link is present in the navigation because the pages are being built, and
          it does not yet lead to a working system.
        </p>
        <p>
          When accounts ship, this section will change and will say so. At that point we
          expect to store an email address, a hashed password or a Google account identifier,
          a display name, and the time the account was created. Venting will still not require
          an account, and account data will still never be connected to vent text, because
          there will still be no vent text to connect it to.
        </p>

        <h2>Cookies</h2>
        <p>
          Today the site sets no analytics or advertising cookies. Once accounts ship there
          will be a session cookie for staying signed in, and there is a small cookie planned
          to remember that you dismissed the sign-in prompt, so it does not reappear.
        </p>
        <p>
          Nothing you write is stored in your browser either. No draft of a vent is kept in
          local storage or session storage, so closing the tab loses it, which is the intended
          behaviour.
        </p>

        <h2>Third parties</h2>
        <p>What other companies receive:</p>
        <ul>
          <li>
            <strong>Our hosting provider</strong> handles the traffic to this site, so it
            processes your IP address as any web host does. It does not receive vent text,
            because vent text is never sent.
          </li>
          <li>
            <strong>Fonts</strong> are served from this site, not from Google Fonts. Loading a
            page does not tell Google that you visited.
          </li>
          <li>
            <strong>No analytics.</strong> There is no Google Analytics, no Meta pixel, no
            heatmap tool, no session recorder on this site.
          </li>
          <li>
            <strong>No advertisers, and no data sold or shared.</strong> There is no
            advertising on this site and nothing here is for sale.
          </li>
          <li>
            When donations are added, a payment provider will handle the transaction and
            receive whatever it needs to process it. That is not built yet, and this section
            will be updated when it is.
          </li>
        </ul>

        <h2>Requests about your data</h2>
        <p>
          Because vent text is never collected, there is nothing to request or erase. For
          feedback you have submitted, you can ask for a copy or for removal by email.
        </p>

        <h2>Changes</h2>
        <p>
          This page changes as the software does. It will be updated when accounts ship, when
          donations ship, and after it has had legal review.
        </p>
      </Prose>

      <p className="mt-10">
        <a
          href={CONTACT_MAILTO}
          className={cn(
            "inline-flex min-h-11 items-center rounded-sm text-body",
            "text-(--color-text-accent) underline decoration-ink-faint underline-offset-4",
            "hover:decoration-clay",
            focusRing,
          )}
        >
          Email a privacy question
        </a>
      </p>

      <p className="mt-8 border-t border-rule pt-5 text-caption text-ink-soft">
        Last updated 4 September 2026. Written against the software as built on that date.
      </p>
    </div>
  );
}
