/**
 * ============================================================================
 * PLACEHOLDER — NOT A REAL ADDRESS. EDIT THE LINE BELOW BEFORE ANY DEPLOY.
 * ============================================================================
 *
 * `/contact` is complete apart from this one value. Replace the string on the
 * `CONTACT_EMAIL` line with the real inbox and nothing else needs to change:
 * the page, the mailto link and the displayed address all read from here.
 *
 * It is deliberately not a plausible-looking address. A `mailto:` pointing at a
 * domain that receives nothing is worse than an obvious placeholder, because it
 * looks like it works and quietly swallows the message — including, on this
 * product, messages from people having a bad day.
 *
 * `CONTACT_EMAIL_IS_PLACEHOLDER` drives a visible notice on the page. Set it to
 * `false` in the same edit, and the notice disappears.
 */

/** ⬇⬇⬇  THE ONE LINE TO EDIT  ⬇⬇⬇ */
export const CONTACT_EMAIL = "REPLACE-ME@example.invalid";
/** ⬆⬆⬆  THE ONE LINE TO EDIT  ⬆⬆⬆ */

/** Set to false in the same edit that supplies a real address above. */
export const CONTACT_EMAIL_IS_PLACEHOLDER = true;

/** Prefilled subject, so a reply thread starts with some context. */
export const CONTACT_MAILTO = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(
  "HeadHeartFreeS enquiry",
)}`;
