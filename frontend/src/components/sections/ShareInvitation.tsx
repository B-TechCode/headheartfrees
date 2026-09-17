import { cn } from "@/lib/cn";
import { focusRing, secondaryAction } from "@/components/ui/styles";
import { SHARE_FORM_IS_PLACEHOLDER, SHARE_FORM_URL } from "@/lib/share";

/**
 * The route to the form for people who want to be read.
 *
 * ===========================================================================
 * Why both copies live in one file
 * ===========================================================================
 *
 * This renders in two places that say the same five things at different
 * lengths: `/vent`, below the composer, and `/vent/released`, below the
 * feedback invitation. Keeping the two next to each other is the point — the
 * five things are a set, and a set maintained in two files is a set that
 * loses a member. They are:
 *
 *   1. it is sent, and it is stored;
 *   2. a person reads it;
 *   3. it goes to Google, not to this site, and Google's terms apply there;
 *   4. it may ask for contact details, which can be left blank;
 *   5. no Google account is needed.
 *
 * Every one of those was checked against the live form rather than assumed.
 * The form is public — it renders and submits with no session — it collects
 * no email address of its own, and both of its contact questions are optional.
 *
 * **Nothing here may be trimmed for tone.** The visitor reading this has been
 * told, in several places on the way down, that nothing is stored and nobody
 * reads it. A link that leads somewhere storing everything, without saying so
 * first, is the site misleading them at the one point where its credibility
 * lives.
 *
 * ===========================================================================
 * Why it is not a button, and not beside "Release & Let Go"
 * ===========================================================================
 *
 * "Release & Let Go" means *this disappears*. A second button beside it that
 * sends the same words to Google muddles that promise at the moment it matters
 * most, and someone moving quickly could press the wrong one. So this sits in
 * its own block, behind a rule, under its own heading, after the line that
 * says nothing has been sent — far enough down that it reads as a different
 * offer rather than as a second submit.
 *
 * `secondaryAction` for the same reason. One filled button per section is what
 * tells someone which action is the main one; on `/vent` that action is
 * releasing. This is the alternative, and it is styled as one.
 *
 * ===========================================================================
 * The claim this deliberately does not repeat
 * ===========================================================================
 *
 * The form's own description promises its reader "100% confidential" and
 * "100% सुरक्षित और गोपनीय". That is the form owner's promise to make, on the
 * form, and it is not echoed here: this site cannot vouch for the handling of
 * data sitting in someone else's Google account, and a guarantee it cannot
 * keep is exactly the kind of sentence the rest of the site avoids. If a later
 * pass tries to "align" this copy with the form's own wording, that is the
 * reason not to. Recorded in PHASE_LOG as a decision rather than left to look
 * like an oversight.
 */
export function ShareInvitation({ variant }: { variant: "vent" | "released" }) {
  /*
   * The placeholder flag renders nothing at all — see `lib/share.ts` for why
   * silence is the right failure here, rather than the visible notice that
   * `/contact` and `/support` use for theirs.
   */
  if (SHARE_FORM_IS_PLACEHOLDER) return null;

  return variant === "vent" ? <VentBlock /> : <ReleasedBlock />;
}

/** `/vent` — below the composer, after "Nothing you have written is sent anywhere". */
function VentBlock() {
  return (
    <section
      aria-labelledby="share-form-heading"
      className="mt-14 border-t border-rule pt-10"
    >
      <h2 id="share-form-heading" className="font-display text-h3 text-ink">
        If you would rather be heard
      </h2>

      <p className="mt-4 max-w-[60ch] text-body text-ink-soft">
        Everything above this line is built to disappear. Sometimes that is not what you
        want &mdash; you want a person to read it, and maybe to answer.
      </p>

      <p className="mt-4 max-w-[60ch] text-body text-ink-soft">
        There is a separate form for that, and it works the other way round. What you write
        there <strong className="font-medium text-ink">is sent, and it is stored.</strong>{" "}
        A person reads it. It is a Google Form, so it goes to Google&rsquo;s servers rather
        than to this site, and Google&rsquo;s terms apply to it once it is there. Near the
        end it asks whether you would like to be contacted about what you wrote, and where
        to reach you &mdash; you can leave both blank and send the rest anyway.
      </p>

      {/*
        The Hindi clause is here and not on /vent/released. Someone who would
        write more freely in Hindi has no way to discover that unless the link
        says so, and a reader who does not need it spends one clause. The
        released page cannot afford the words — see the note in ReleasedBlock.
      */}
      <p className="mt-4 max-w-[60ch] text-body-sm text-ink-soft">
        You do not need a Google account. The questions are in English and Hindi.
      </p>

      <ShareFormLink />
    </section>
  );
}

/** `/vent/released` — below "Leave a note", above the line about running costs. */
function ReleasedBlock() {
  return (
    <section
      aria-labelledby="share-form-heading"
      className="mt-16 border-t border-rule pt-8"
    >
      <h2 id="share-form-heading" className="font-display text-h4 text-ink">
        If you wanted someone to read it
      </h2>

      {/*
        The opening clause is the one this page needs and /vent does not: the
        words are already gone by the time anyone reads this. Nobody must open
        the form from here expecting to find what they wrote waiting in it.
        Naming the loss and removing the urgency in the same sentence is the
        whole job of "whenever you feel like it".

        Shorter than the /vent copy, and deliberately. This page sits seconds
        after a release and is quiet on purpose; the five things still all
        appear, compressed into one paragraph rather than three.
      */}
      <p className="mt-3 max-w-[58ch] text-body text-ink-soft">
        What you wrote is gone, so this would be starting again, from a blank page,
        whenever you feel like it. There is a form for people who would rather be heard
        than let go, and it is the opposite of this one: it is sent, it is stored, and a
        person reads it. It is a Google Form, so it goes to Google rather than to this
        site, and Google&rsquo;s terms apply there. It can ask where to reach you if you
        want a reply &mdash; or you can leave that blank.
      </p>

      <p className="mt-3 max-w-[58ch] text-body-sm text-ink-soft">
        No Google account needed.
      </p>

      <ShareFormLink />
    </section>
  );
}

/**
 * The link itself. One definition, two call sites.
 *
 * - `target="_blank"` with `rel="noopener noreferrer"`, as the footer's social
 *   links do. `noopener` is what stops the opened page reaching back through
 *   `window.opener`.
 * - The destination is named in the link text rather than left to the URL.
 *   "Open the form" would not tell anyone they are leaving this site; "on
 *   Google Forms" does, and it is read out as part of the accessible name.
 * - The glyph and the "(opens in a new tab)" hint both sit inside the anchor.
 *   The hint is visible rather than screen-reader-only on purpose: the ask is
 *   that leaving the site be obvious, and a fact only a screen reader hears is
 *   obvious to a small fraction of visitors.
 * - `inline-block py-2.5` rather than the `inline-flex min-h-11` used
 *   elsewhere: the label then wraps as ordinary text rather than sitting as
 *   three boxes in a row that cannot break, which matters at 390px where it
 *   takes two lines. 16px of text at 1.65 line-height plus 20px of padding is
 *   46.4px measured, so the 44px target from PROJECT_BRIEF.md §8 is met
 *   without the flex box. The underline from `secondaryAction` runs under the
 *   text on both sides of the glyph and skips the glyph itself, which is the
 *   browser's own handling of a replaced element and reads correctly.
 */
function ShareFormLink() {
  return (
    <p className="mt-5">
      <a
        href={SHARE_FORM_URL}
        target="_blank"
        rel="noopener noreferrer"
        className={cn("inline-block py-2.5 text-body", secondaryAction, focusRing)}
      >
        Open the form on Google Forms
        <ExternalArrow />
        <span className="text-body-sm"> (opens in a new tab)</span>
      </a>
    </p>
  );
}

/**
 * The house icon spec: `0 0 24 24`, `fill="none"`, 1.75 stroke in
 * `currentColor`, round caps and joins — the same numbers the navbar mark and
 * the footer glyphs are drawn to.
 *
 * `aria-hidden` because the anchor already says "opens in a new tab" in text.
 * An arrow carrying a label of its own would announce the same fact twice.
 */
function ExternalArrow() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="ml-1.5 inline-block h-4 w-4 align-[-0.1em]"
    >
      <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
      <path d="M15 3h6v6" />
      <path d="M10 14 21 3" />
    </svg>
  );
}
