import type { ComponentProps } from "react";
import { cn } from "@/lib/cn";
import { disabledControl, focusRing } from "./styles";

// ComponentProps rather than TextareaHTMLAttributes: React 19 treats ref as an
// ordinary prop on function components, and only ComponentProps includes it in
// the type. The vent composer needs a ref to place the caret after inserting a
// prompt starter.
export type TextareaProps = ComponentProps<"textarea"> & {
  invalid?: boolean;
};

/**
 * Multi-line text input.
 *
 * This is the primitive behind the vent textarea in Phase 6, so two things are
 * set here rather than patched on later:
 *
 * - `field-sizing-content` lets the box grow with what is typed instead of
 *   forcing a scrollbar inside a fixed rectangle. Browsers without it fall back
 *   to the `rows` default, which is why `min-h` is set too.
 * - The line height is looser than the rest of the UI. Someone writing about a
 *   hard day should not be reading their own words out of a dense block.
 *
 * Rule §2.1 note: nothing in this component, or anything built on it, may send
 * its value anywhere. It is an uncontrolled or parent-controlled field and has
 * no network behaviour of its own.
 */
export function Textarea({ invalid = false, className, rows = 6, ...props }: TextareaProps) {
  return (
    <textarea
      rows={rows}
      aria-invalid={invalid || undefined}
      className={cn(
        "w-full rounded-lg px-3.5 py-3",
        "font-sans text-body-lg leading-[1.75] text-ink",
        "bg-surface-raised border",
        invalid ? "border-danger" : "border-ink-faint",
        "placeholder:text-ink-faint",
        "min-h-36 [field-sizing:content] resize-y",
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
