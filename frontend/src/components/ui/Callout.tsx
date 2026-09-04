import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

export type CalloutTone = "note" | "important" | "caution";

/**
 * A short aside that has to be read, not skimmed past.
 *
 * Three tones, none of them red. `caution` uses the warning token for things
 * that are genuinely provisional, like a policy awaiting legal review — but
 * nothing here uses `danger`, because on this product an alarm-coloured panel
 * is almost always the wrong instrument. The one place real urgency belongs is
 * a helpline, and that is styled as steady rather than loud.
 */
const toneClasses: Record<CalloutTone, string> = {
  note: "bg-surface-sunk border-rule",
  important: "bg-surface-raised border-rule",
  caution: "bg-surface-raised border-warning/40",
};

/** A short rule above the title. The accent earns its place on `important` only. */
const ruleClasses: Record<CalloutTone, string> = {
  note: "bg-rule-strong",
  important: "bg-clay",
  caution: "bg-warning",
};

export type CalloutProps = {
  tone?: CalloutTone;
  title?: string;
  children: ReactNode;
  className?: string;
};

export function Callout({ tone = "note", title, children, className }: CalloutProps) {
  return (
    <aside className={cn("rounded-lg border p-5 sm:p-6", toneClasses[tone], className)}>
      <span aria-hidden="true" className={cn("block h-px w-10", ruleClasses[tone])} />
      {title ? (
        <h2 className="mt-4 font-display text-h4 text-ink">{title}</h2>
      ) : null}
      <div className={cn("text-body text-ink-soft", title ? "mt-2" : "mt-4")}>{children}</div>
    </aside>
  );
}
