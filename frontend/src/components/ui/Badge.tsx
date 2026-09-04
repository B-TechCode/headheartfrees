import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

export type BadgeTone = "neutral" | "accent" | "success" | "warning" | "danger" | "info";

/**
 * Status colours are used as text on a tinted rule, not as a saturated fill.
 * At badge sizes a solid `--color-warning` block is louder than anything on
 * this product's pages should be, and the text-on-wash treatment keeps every
 * tone at AA without needing six separate foreground tokens.
 */
const toneClasses: Record<BadgeTone, string> = {
  neutral: "text-ink-soft border-rule-strong bg-surface-sunk",
  accent: "text-(--color-text-accent) border-clay bg-clay-wash",
  success: "text-success border-success/35 bg-success/4",
  warning: "text-warning border-warning/35 bg-warning/4",
  danger: "text-danger border-danger/35 bg-danger/4",
  info: "text-info border-info/35 bg-info/4",
};

export type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: BadgeTone;
  children?: ReactNode;
};

/**
 * Non-interactive status label. If it needs a click, it is a {@link Chip}.
 */
export function Badge({ tone = "neutral", className, children, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-sm border px-2 py-0.5",
        "font-sans text-overline font-semibold uppercase",
        toneClasses[tone],
        className,
      )}
      {...props}
    >
      {children}
    </span>
  );
}
