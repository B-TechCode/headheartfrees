import type { LabelHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

export type LabelProps = LabelHTMLAttributes<HTMLLabelElement> & {
  /**
   * Marks the field optional in the visible label.
   *
   * Optional is marked rather than required, which is the inverse of the usual
   * asterisk convention and deliberate for this product: on the feedback and
   * deeper-share forms most fields genuinely are optional, and a page of
   * asterisks reads as a form that wants things from you.
   */
  optional?: boolean;
  children?: ReactNode;
};

export function Label({ optional = false, className, children, ...props }: LabelProps) {
  return (
    <label
      className={cn(
        "font-sans text-body-sm font-medium text-ink",
        "inline-flex items-baseline gap-2",
        className,
      )}
      {...props}
    >
      {children}
      {optional ? (
        <span className="text-caption font-normal text-ink-soft">optional</span>
      ) : null}
    </label>
  );
}
