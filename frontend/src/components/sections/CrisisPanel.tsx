import Link from "next/link";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { FOOTER_HELPLINES } from "@/lib/helplines";

/**
 * The inline panel shown above the release button when `lib/safety.ts` matches.
 *
 * Tone is the whole design. This appears while someone is mid-sentence about
 * the worst thing in their life, and the wrong version of it does harm:
 *
 * - **No red, no warning icon, no alarm.** Same treatment as the footer strip:
 *   `surface-raised` with a accent hairline. A light left on, not a siren.
 * - **It never names a method.** Some of the trigger phrases in `safety.ts` do;
 *   the copy here does not repeat them back, and must not be edited to.
 * - **It does not diagnose.** No "it sounds like you are in crisis". It says
 *   the numbers exist and that they are answered by people.
 * - **It does not block anything.** The release button stays enabled, nothing
 *   is gated, and the panel has no dismiss control because dismissing it would
 *   imply it was in the way.
 *
 * Numbers come from `lib/helplines.ts`, never hardcoded. That file is the
 * single source of truth, and it exists because a hardcoded number went stale
 * once already.
 *
 * `role="status"` rather than `alert`: polite, announced at the next natural
 * pause instead of interrupting. A screen reader user typing about something
 * painful should not have their sentence cut into.
 */
export function CrisisPanel() {
  return (
    <section
      role="status"
      aria-labelledby="crisis-panel-heading"
      className="rounded-lg border border-rule bg-surface-raised p-5 sm:p-6"
    >
      <span aria-hidden="true" className="block h-px w-10 bg-accent" />

      <h2 id="crisis-panel-heading" className="mt-4 font-display text-h4 text-ink">
        These lines are answered by people.
      </h2>

      <p className="mt-2 max-w-[60ch] text-body text-ink-soft">
        Free, confidential, and open to anyone. You do not have to be in an emergency to
        call one, and you do not have to explain yourself well.
      </p>

      <ul className="mt-4 flex flex-col gap-1">
        {FOOTER_HELPLINES.map((line) => (
          <li key={line.name}>
            <a
              href={line.href}
              className={cn(
                "group flex min-h-11 flex-wrap items-baseline gap-x-2 rounded-sm py-1",
                "text-body text-ink transition-colors duration-150 ease-out",
                focusRing,
              )}
            >
              <span className="font-medium">{line.name}</span>
              {line.prefix ? (
                <span className="text-body-sm text-ink-soft">{line.prefix}</span>
              ) : null}
              <span
                className={cn(
                  "underline decoration-ink-faint decoration-from-font underline-offset-4",
                  "group-hover:text-(--color-text-accent) group-hover:decoration-accent",
                )}
              >
                {line.number}
              </span>
              <span className="text-caption text-ink-soft">{line.hours}</span>
            </a>
          </li>
        ))}
      </ul>

      <p className="mt-4 text-body-sm text-ink-soft">
        <Link
          href="/crisis-resources"
          className={cn(
            "rounded-sm underline decoration-ink-faint underline-offset-4",
            "hover:text-(--color-text-accent) hover:decoration-accent",
            focusRing,
          )}
        >
          More lines, including outside India
        </Link>
      </p>

      <p className="mt-4 text-body-sm text-ink-soft">
        Nothing you have written has been sent anywhere. This is still your page, and the
        button below still works exactly as it did.
      </p>
    </section>
  );
}
