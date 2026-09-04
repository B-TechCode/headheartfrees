import Link from "next/link";
import { cn } from "@/lib/cn";
import { Logo } from "@/components/ui/Logo";
import { focusRing } from "@/components/ui/styles";

/**
 * PROJECT_BRIEF.md §7: Donate and Feedback are removed from the Navigate
 * column. They were in the old design and must not come back.
 */
const NAVIGATE_LINKS = [
  { href: "/", label: "Home" },
  { href: "/vent", label: "Vent" },
  { href: "/about", label: "About" },
] as const;

const SUPPORT_LINKS = [
  { href: "/crisis-resources", label: "Crisis Resources" },
  { href: "/community-guidelines", label: "Community Guidelines" },
  { href: "/privacy", label: "Privacy Policy" },
  { href: "/contact", label: "Contact Us" },
] as const;

/**
 * Helplines, in the order someone in India would actually reach for them.
 *
 * Crisis Text Line wants an SMS rather than a call. A prefilled `sms:` body
 * behaves differently on iOS (`&body=`) and Android (`?body=`) and fails
 * silently on desktop, so the link is a plain `sms:` to the number and the
 * "text HOME to" instruction is carried as visible text instead.
 */
const HELPLINES = [
  { name: "iCall", action: "9152987821", href: "tel:9152987821", prefix: null },
  {
    name: "Vandrevala Foundation",
    action: "1860-2662-345",
    href: "tel:18602662345",
    prefix: null,
  },
  {
    name: "Crisis Text Line",
    action: "741741",
    href: "sms:741741",
    prefix: "text HOME to",
  },
] as const;

export function Footer() {
  return (
    <footer className="app-layer mt-auto border-t border-rule bg-surface">
      <CrisisStrip />

      {/*
        Asymmetric on purpose: the brand column is twice the width of either
        link column, so this does not read as an evenly divided four-up grid.
      */}
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr] lg:gap-12">
          <div className="max-w-sm">
            <Logo title={null} className="h-8 w-8 text-ink" />
            <p className="mt-4 font-display text-h4 text-ink">A place to put it down.</p>
            <p className="mt-2 text-body-sm text-ink-soft">
              What you write here never leaves your browser. It is not stored, not sent, and
              not read by anyone — including us.
            </p>
          </div>

          <FooterColumn title="Navigate" links={NAVIGATE_LINKS} />
          <FooterColumn title="Support" links={SUPPORT_LINKS} />
        </div>

        <div
          className={cn(
            "mt-12 flex flex-col gap-4 border-t border-rule pt-6",
            "sm:flex-row sm:items-center sm:justify-between",
          )}
        >
          <p className="text-caption text-ink-soft">
            &copy; {new Date().getFullYear()} HeadHeartFreeS
          </p>

          {/* Quiet by design — a soft line, not a banner and not a modal. */}
          <Link
            href="/support"
            className={cn(
              "inline-flex min-h-11 items-center rounded-sm text-body-sm text-ink-soft",
              "underline decoration-rule-strong underline-offset-4",
              "transition-colors duration-150 ease-out",
              "hover:text-(--color-text-accent) hover:decoration-clay",
              focusRing,
            )}
          >
            Support this space
          </Link>
        </div>
      </div>
    </footer>
  );
}

/**
 * The helpline strip.
 *
 * This is the most important element on the page and it is built that way: body
 * text at full ink contrast (14.9:1), not caption grey; every number a real
 * link with a 44px target; and enough room around it that it is not skimmed
 * past.
 *
 * What it deliberately is *not*: red, bordered in danger colour, prefixed with
 * a warning icon, or headed with the word "crisis". Someone reaching this strip
 * is having a hard enough day without the page raising its voice at them. It
 * should read as steady and available — the tone of a light left on, not an
 * alarm. `surface-raised` plus a clay hairline gives it presence without
 * urgency.
 */
function CrisisStrip() {
  return (
    <section
      aria-labelledby="helplines-heading"
      className="border-b border-rule bg-surface-raised"
    >
      <div className="mx-auto max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
        {/* A single clay hairline: present, not loud. */}
        <span aria-hidden="true" className="block h-px w-10 bg-clay" />

        <h2 id="helplines-heading" className="mt-4 font-display text-h4 text-ink">
          If you would rather talk to someone, these lines are open.
        </h2>
        <p className="mt-1 text-body-sm text-ink-soft">
          Free, confidential, and staffed by people who do this every day.
        </p>

        <ul className="mt-5 flex flex-col gap-1 sm:flex-row sm:flex-wrap sm:gap-x-10 sm:gap-y-1">
          {HELPLINES.map((line) => (
            <li key={line.name}>
              <a
                href={line.href}
                className={cn(
                  "group flex min-h-11 flex-wrap items-baseline gap-x-2 rounded-sm py-1",
                  "text-body text-ink",
                  "transition-colors duration-150 ease-out",
                  focusRing,
                )}
              >
                <span className="font-medium">{line.name}</span>
                {line.prefix ? (
                  <span className="text-body-sm text-ink-soft">{line.prefix}</span>
                ) : null}
                {/*
                  A visible underline, not a hairline. `rule-strong` is 1.88:1
                  against this surface — legible enough for a decorative
                  divider, too faint to advertise that a phone number is
                  tappable. On this strip in particular the affordance has to
                  be obvious.
                */}
                <span
                  className={cn(
                    "underline decoration-ink-faint decoration-from-font underline-offset-4",
                    "group-hover:text-(--color-text-accent) group-hover:decoration-clay",
                  )}
                >
                  {line.action}
                </span>
              </a>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function FooterColumn({
  title,
  links,
}: {
  title: string;
  links: ReadonlyArray<{ href: string; label: string }>;
}) {
  return (
    <div>
      <h2 className="font-sans text-overline font-semibold tracking-[0.085em] text-ink-soft uppercase">
        {title}
      </h2>
      <ul className="mt-3 flex flex-col">
        {links.map((link) => (
          <li key={link.href}>
            <Link
              href={link.href}
              className={cn(
                "inline-flex min-h-11 items-center rounded-sm text-body-sm text-ink-soft",
                "transition-colors duration-150 ease-out hover:text-ink",
                focusRing,
              )}
            >
              {link.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
