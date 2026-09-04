import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

export type CardTone = "raised" | "sunk" | "outline";

/**
 * Hierarchy comes from the surface tokens and a hairline rule — never a drop
 * shadow. PROJECT_BRIEF.md §8 names "default radii and drop shadows repeated on
 * every card" as the first tell of a generated interface, and a page of
 * identically floating cards is exactly that.
 *
 * `raised` lifts off the page, `sunk` recedes into it, `outline` sits flat on
 * it. Three options, so a layout has to choose rather than defaulting.
 */
const toneClasses: Record<CardTone, string> = {
  raised: "bg-surface-raised border-rule",
  sunk: "bg-surface-sunk border-transparent",
  outline: "bg-transparent border-rule",
};

export type CardProps = HTMLAttributes<HTMLDivElement> & {
  tone?: CardTone;
  /** Renders as `<article>` when the card is a self-contained item in a list. */
  as?: "div" | "article";
  children?: ReactNode;
};

export function Card({
  tone = "raised",
  as: Component = "div",
  className,
  children,
  ...props
}: CardProps) {
  return (
    <Component
      className={cn("rounded-lg border p-5 sm:p-6", toneClasses[tone], className)}
      {...props}
    >
      {children}
    </Component>
  );
}

export function CardHeader({ className, children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("mb-3 flex flex-col gap-1", className)} {...props}>
      {children}
    </div>
  );
}

export function CardTitle({ className, children, ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h3 className={cn("font-display text-h4 text-ink", className)} {...props}>
      {children}
    </h3>
  );
}

export function CardBody({ className, children, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn("text-body text-ink-soft", className)} {...props}>
      {children}
    </div>
  );
}
