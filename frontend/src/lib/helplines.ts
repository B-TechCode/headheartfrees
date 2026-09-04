/**
 * Crisis helplines — the single source of truth.
 *
 * This file exists because the numbers drifted once already. The footer carried
 * Vandrevala Foundation as 1860-2662-345 for two phases; that number appears on
 * none of the foundation's current pages, and it was caught only because Phase 3
 * happened to re-verify. Every surface that shows a helpline now reads from here:
 * the footer strip, `/crisis-resources`, and the in-browser crisis panel in
 * Phase 6. One list, one place to correct.
 *
 * Rules for editing:
 *
 * 1. Verify against the operator's OWN site, not a directory or a news article.
 *    Record where in `verifiedFrom`.
 * 2. `hours` is required and must never be softened. A line that is not open is
 *    worse than a line that is not listed, because someone dials it and gets
 *    nothing at the moment they needed something.
 * 3. Order by availability. Around-the-clock services first.
 * 4. If you cannot confirm a number, leave it out.
 *
 * Last verified: 2026-09-04.
 */

export type Helpline = {
  /** Operator name, as they write it. */
  name: string;
  /** Shown exactly as a person should dial or text it. */
  number: string;
  /** `tel:` or `sms:` URI. Digits only, no punctuation. */
  href: string;
  /** Required. Displayed on every entry, everywhere. */
  hours: string;
  /** One line on what this is or who answers. */
  note?: string;
  /** Rendered before the number, e.g. "text HOME to". */
  prefix?: string;
  /** Flags a landline, which matters to mobile-only and out-of-state callers. */
  landline?: boolean;
  /** Where the number was confirmed. Not rendered; kept for the next audit. */
  verifiedFrom: string;
};

/**
 * India. Tele-MANAS leads deliberately: it is free, government-run, staffed
 * around the clock in 20 languages, and 14416 is a five-digit short code, which
 * is the one thing on this page a person might actually retain while distressed.
 */
export const INDIA_HELPLINES: readonly Helpline[] = [
  {
    name: "Tele-MANAS",
    number: "14416",
    href: "tel:14416",
    hours: "24 hours, every day",
    note: "Government of India. Free, in 20 languages. Also on 1800-891-4416.",
    verifiedFrom: "telemanas.mohfw.gov.in and JIPMER",
  },
  {
    name: "Vandrevala Foundation",
    number: "9999666555",
    href: "tel:9999666555",
    hours: "24 hours, every day",
    note: "Free counselling by phone or WhatsApp on the same number.",
    verifiedFrom: "vandrevalafoundation.com contact page",
  },
  {
    name: "AASRA",
    number: "022-27546669",
    href: "tel:02227546669",
    hours: "24 hours, every day",
    note: "Trained volunteers, in English and Hindi.",
    landline: true,
    verifiedFrom: "aasra.info contact page",
  },
  {
    name: "SNEHA",
    number: "044-24640050",
    href: "tel:04424640050",
    hours: "24 hours, every day",
    note: "Chennai. Suicide prevention, open to callers anywhere.",
    landline: true,
    verifiedFrom: "snehaindia.org",
  },
  {
    name: "iCall",
    number: "9152987821",
    href: "tel:9152987821",
    hours: "Monday to Saturday, 10am to 8pm",
    note: "TISS Mumbai. Counsellors in eight languages. Not open overnight.",
    verifiedFrom: "icallhelpline.org telephone counselling page",
  },
];

/** Outside India. */
export const INTERNATIONAL_HELPLINES: readonly Helpline[] = [
  {
    name: "Crisis Text Line",
    number: "741741",
    href: "sms:741741",
    prefix: "text HOME to",
    hours: "24 hours, every day",
    note: "United States. Canada 686868, United Kingdom 85258, Ireland 50808.",
    verifiedFrom: "crisistextline.org",
  },
];

/**
 * The four shown in the footer on every page. A deliberate subset — the strip
 * has to stay skimmable — but drawn from the same list so it cannot drift.
 */
export const FOOTER_HELPLINE_NAMES = [
  "Tele-MANAS",
  "Vandrevala Foundation",
  "iCall",
] as const;

export const FOOTER_HELPLINES: readonly Helpline[] = [
  ...INDIA_HELPLINES.filter((line) => FOOTER_HELPLINE_NAMES.some((n) => n === line.name)),
  ...INTERNATIONAL_HELPLINES,
];
