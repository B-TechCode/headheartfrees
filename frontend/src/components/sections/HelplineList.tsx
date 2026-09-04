import type { Helpline } from "@/lib/helplines";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";

/**
 * Helplines rendered as the primary content of a page, rather than as the
 * compact footer strip.
 *
 * Built to be scanned, not read. Someone arriving here may be in a bad state,
 * so the number is the largest thing in each row, the hours sit immediately
 * beside it, and the description comes last. Nothing above the numbers needs
 * to be read before dialling.
 *
 * Every row is a single link covering the whole block, so the target is far
 * larger than the 44px minimum rather than exactly meeting it.
 */
export function HelplineList({ helplines }: { helplines: readonly Helpline[] }) {
  return (
    <ul className="mt-6 flex flex-col gap-3">
      {helplines.map((line) => (
        <li key={line.name}>
          <a
            href={line.href}
            className={cn(
              "group block rounded-lg border border-rule bg-surface-raised",
              "px-5 py-4 sm:px-6 sm:py-5",
              "transition-[border-color,background-color] duration-150 ease-out",
              "hover:border-clay",
              focusRing,
            )}
          >
            <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
              <span
                className={cn(
                  "font-display text-h3 text-ink",
                  "underline decoration-ink-faint decoration-from-font underline-offset-[6px]",
                  "group-hover:decoration-clay",
                )}
              >
                {line.prefix ? (
                  <span className="text-h4 font-normal text-ink-soft">{line.prefix} </span>
                ) : null}
                {line.number}
              </span>

              <span className="text-body-sm font-medium text-ink">{line.hours}</span>

              {line.landline ? (
                <span className="text-caption text-ink-soft">landline</span>
              ) : null}
            </div>

            <p className="mt-1 text-body text-ink-soft">
              <span className="font-medium text-ink">{line.name}</span>
              {line.note ? <span> &middot; {line.note}</span> : null}
            </p>
          </a>
        </li>
      ))}
    </ul>
  );
}
