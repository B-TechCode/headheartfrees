import type { InputHTMLAttributes } from "react";
import { cn } from "@/lib/cn";
import { disabledControl, focusRing } from "./styles";

export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  /**
   * Renders the error state. The message itself belongs to `FormField`, which
   * also wires up `aria-describedby`.
   */
  invalid?: boolean;
};

/**
 * Single-line text input.
 *
 * Sits on `surface-raised` against the `surface` page, so the field reads as a
 * well cut into paper rather than a box drawn on top of it — which is what a
 * border plus a shadow would give.
 */
export function Input({ invalid = false, className, ...props }: InputProps) {
  return (
    <input
      aria-invalid={invalid || undefined}
      className={cn(
        "h-12 w-full rounded-md px-3.5",
        "font-sans text-body text-ink",
        "bg-surface-raised border",
        invalid ? "border-danger" : "border-ink-faint",
        "placeholder:text-ink-faint",
        "transition-[border-color,background-color] duration-150 ease-out",
        "enabled:hover:border-ink-soft",
        focusRing,
        disabledControl,
        className,
      )}
      {...props}
    />
  );
}
