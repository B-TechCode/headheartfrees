/**
 * ============================================================================
 * PLACEHOLDER — NOT A REAL PAYMENT ADDRESS. EDIT THE LINE BELOW BEFORE ANY
 * DEPLOY THAT INTENDS TO ACCEPT MONEY.
 * ============================================================================
 *
 * `/support` is complete apart from this one value. Replace the string on the
 * `SUPPORT_UPI_ID` line with the owner's real UPI ID and nothing else needs to
 * change: the displayed address, the `upi://` link and the notice all read
 * from here.
 *
 * It is deliberately not a plausible-looking identifier, for the same reason
 * `lib/contact.ts` uses `@example.invalid`. A UPI ID that looks real but is
 * not is worse than an obvious placeholder: money sent to a valid-but-wrong
 * VPA reaches somebody, and it will not be the person running this site.
 *
 * **There are two lines to change, not one.** `SUPPORT_PAYMENT_IS_PLACEHOLDER`
 * below drives a visible notice telling visitors that nothing can be sent yet.
 * Setting a real ID without clearing the flag leaves the notice up and the
 * payment details hidden; clearing the flag without setting a real ID publishes
 * an invalid address. Do both in the same edit.
 *
 * UPI needs no integration, no SDK and no API key — the ID and a link are the
 * whole mechanism. That is why this file has no provider configuration in it.
 *
 * @see HANDOVER.md section 2.7
 */

/** ⬇⬇⬇  THE ONE LINE TO EDIT  ⬇⬇⬇ */
export const SUPPORT_UPI_ID = "REPLACE-ME@example.invalid";
/** ⬆⬆⬆  THE ONE LINE TO EDIT  ⬆⬆⬆ */

/**
 * Set to `false` in the same edit that supplies a real ID above.
 *
 * While `true`, `/support` shows a notice saying nothing can be given yet and
 * renders no payment details at all. Polarity follows
 * `CONTACT_EMAIL_IS_PLACEHOLDER` in `lib/contact.ts`, which is now `false`
 * because a real address was supplied. This one is still waiting.
 */
export const SUPPORT_PAYMENT_IS_PLACEHOLDER = true;

/** The name a UPI app shows the sender as the payee. */
export const SUPPORT_PAYEE_NAME = "HeadHeartFreeS";

/**
 * The `upi://` deep link, derived rather than written out separately.
 *
 * Two places holding the same identifier is how one of them ends up stale, so
 * the link is built from the constant above.
 *
 * **No amount is included, deliberately.** `upi://` accepts an `am` parameter
 * that prefills a figure in the payment app, and using it would be a suggested
 * amount presented as a default — the exact thing this page is not allowed to
 * do. The sender types whatever they type.
 */
export const SUPPORT_UPI_LINK =
  `upi://pay?pa=${encodeURIComponent(SUPPORT_UPI_ID)}` +
  `&pn=${encodeURIComponent(SUPPORT_PAYEE_NAME)}` +
  `&cu=INR`;
