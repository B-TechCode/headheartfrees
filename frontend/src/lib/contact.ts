/**
 * The contact address.
 *
 * `/contact` reads everything from here: the displayed address, the mailto
 * link, and whether the page carries a notice saying no inbox is connected
 * yet. That notice was the safety net while this was a placeholder — a
 * `mailto:` pointing at a domain that receives nothing is worse than an
 * obvious placeholder, because it looks like it works and quietly swallows
 * the message, including, on this product, messages from people having a bad
 * day.
 *
 * The address below is real and the notice is off. The account belongs to the
 * project rather than to any one person, so it transfers with the site.
 *
 * If this ever has to change, change both values together. A real address with
 * the flag left `true` strikes the address through and tells visitors not to
 * write; a dead address with the flag `false` takes the warning away.
 */

export const CONTACT_EMAIL = "headheartfrees@gmail.com";

/** False because the address above is a real inbox that a person reads. */
export const CONTACT_EMAIL_IS_PLACEHOLDER = false;

/** Prefilled subject, so a reply thread starts with some context. */
export const CONTACT_MAILTO = `mailto:${CONTACT_EMAIL}?subject=${encodeURIComponent(
  "HeadHeartFreeS enquiry",
)}`;
