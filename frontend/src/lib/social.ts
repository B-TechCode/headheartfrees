/**
 * ============================================================================
 * PLACEHOLDER — THESE ARE THE DEVELOPER'S PERSONAL ACCOUNTS.
 * ============================================================================
 *
 * Every URL below belongs to the person who built this site, not to the
 * project. They are in the footer of every page so the site does not ship with
 * four dead links, and they are **temporary**. At handover they must be
 * replaced with the project owner's own accounts, or removed.
 *
 * This is the third of the three owner-supplied values in this codebase, and
 * it is the one that behaves differently from the other two:
 *
 * - `CONTACT_EMAIL` and `SUPPORT_UPI_ID` are obviously fake strings, and while
 *   their flags are `true` the pages carrying them show a visible notice.
 *   A visitor cannot mistake a half-configured page for a working one.
 * - **These are real, live, working links.** Nothing on the rendered page
 *   marks them as temporary, because there is no honest way to: a footer that
 *   said "these profiles are the developer's" would be strange to a visitor
 *   and would not make anyone click less. So the guard is this comment,
 *   `SOCIAL_LINKS_ARE_PLACEHOLDER`, and HANDOVER.md §2.9 — nothing else.
 *
 * The practical consequence: the other two placeholders fail loudly if
 * forgotten, and this one fails silently. A site handed over with these still
 * in it points its visitors at a stranger's LinkedIn and keeps doing so
 * indefinitely. Check this file at handover even if nothing else prompts you.
 *
 * @see HANDOVER.md section 2.9
 * @see HANDOVER.md section 4
 */

/** The glyph drawn for a link. See `SocialIcon` in `components/layout/Footer.tsx`. */
export type SocialIconName = "briefcase" | "people" | "branch" | "globe";

export type SocialLink = {
  /**
   * The accessible name, and the only name this link has. It is read out as
   * written, so it must say where the link goes — "LinkedIn", never "social
   * link" and never "profile".
   */
  readonly label: string;
  readonly href: string;
  readonly icon: SocialIconName;
};

/** ⬇⬇⬇  THE FOUR LINES TO REPLACE AT HANDOVER  ⬇⬇⬇ */
export const SOCIAL_LINKS: readonly SocialLink[] = [
  {
    label: "LinkedIn",
    href: "https://www.linkedin.com/in/aakashprasadchaurasiya/",
    icon: "briefcase",
  },
  {
    label: "Facebook",
    href: "https://www.facebook.com/aakash.chaurasiya.232668/",
    icon: "people",
  },
  { label: "GitHub", href: "https://github.com/B-TechCode", icon: "branch" },
  { label: "Portfolio", href: "https://www.aakashchaurasiya.com.np/", icon: "globe" },
] as const;
/** ⬆⬆⬆  THE FOUR LINES TO REPLACE AT HANDOVER  ⬆⬆⬆ */

/**
 * Set to `false` once the links above are the project's own.
 *
 * Unlike `CONTACT_EMAIL_IS_PLACEHOLDER` and `SUPPORT_PAYMENT_IS_PLACEHOLDER`,
 * **this flag changes nothing on screen.** It is a marker for whoever reads
 * the source, and a single thing to grep for at handover. It is deliberately
 * not wired to a visitor-facing notice — see the header above for why.
 */
export const SOCIAL_LINKS_ARE_PLACEHOLDER = true;
