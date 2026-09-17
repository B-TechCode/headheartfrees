/**
 * The form for the other case — the one where someone wants to be read.
 *
 * ===========================================================================
 * What this is, and why it is the opposite of everything else on /vent
 * ===========================================================================
 *
 * The vent box stores nothing. That claim is made on the home page, on
 * `/vent`, on `/vent/released`, on `/privacy` and in `HowItWorks`, and it is
 * the only thing this site asks anyone to believe. This URL leads somewhere
 * that stores everything, on Google's servers, where a person reads it.
 *
 * Which is a legitimate second offer, and it is not the same offer. The
 * constraint that follows is the whole reason this file has a header comment
 * at all: **anywhere this link is rendered, the difference is stated before
 * it.** Sent, stored, read by a person, on Google rather than here, and it
 * asks for contact details that can be left blank. A bare "there is also a
 * form" link would spend the site's credibility at the one place it lives.
 *
 * `ShareInvitation` in `components/sections/ShareInvitation.tsx` is the only
 * thing that renders this URL, and it carries that copy. If a third placement
 * is ever wanted, add a variant there rather than importing the constant
 * somewhere new.
 *
 * ===========================================================================
 * A link, never an embed
 * ===========================================================================
 *
 * This is not put in an iframe, and it must not be. Three separate reasons,
 * any one of which is sufficient:
 *
 * - The CSP has no `frame-src` and `default-src 'self'` would block it. Adding
 *   the directive to make an embed work would widen the policy for the whole
 *   site, permanently, for one page.
 * - A Google document rendered inside a page that promises nothing leaves the
 *   browser is a contradiction on screen, whatever the copy around it says.
 * - A visitor who has left this site should be able to tell that they have.
 *   A new tab, a Google URL bar and Google's own chrome say so better than any
 *   sentence could.
 *
 * ===========================================================================
 * Ownership — this is not the site's form
 * ===========================================================================
 *
 * The form belongs to whoever created it, and the responses land in that
 * person's Google Drive. It does not transfer with the site unless somebody
 * transfers it, and a handover that misses it leaves the new owner with a
 * route on their site feeding an inbox they cannot open. That is HANDOVER.md
 * §2.10, and the §4 revocation table repeats it.
 *
 * @see HANDOVER.md section 2.10
 * @see HANDOVER.md section 4
 */

/** The live form. Public — no Google account is needed to open or submit it. */
export const SHARE_FORM_URL =
  "https://docs.google.com/forms/d/e/1FAIpQLSd9IRrw870l0JLS6H2B074NUTIoHX7VA5Qm0LPhPbiX6QGzrQ/viewform";

/**
 * False because the URL above is a real, working, publicly submittable form.
 *
 * Polarity follows `CONTACT_EMAIL_IS_PLACEHOLDER` in `lib/contact.ts`. What it
 * does when `true` is different, and deliberately so: `/contact` keeps its
 * address on screen behind a notice, because a struck-through address still
 * tells a visitor what is coming. **Here, `true` renders nothing at all** —
 * no heading, no rule, no link, on either page.
 *
 * There is no useful half-state. A visitor who has just been told that this
 * one is sent and stored and read by a person, and then finds the link dead or
 * pointing at a form that is not the owner's, has been offered something the
 * site cannot deliver at the moment they decided to trust it. Silence is the
 * honest failure mode, and it costs nothing: the vent box, which is the
 * product, is unaffected.
 */
export const SHARE_FORM_IS_PLACEHOLDER = false;
