import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";
import { disabledControl, focusRing } from "./styles";

export type ChipProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, "aria-pressed"> & {
  /**
   * Selection is owned by the parent. The chip renders `aria-pressed`, which is
   * the correct role for a toggle that does not navigate or submit.
   */
  selected?: boolean;
  /**
   * Leading line icon. Empty in Phase 2 by design: the mood icons are Phase 6
   * content, and §8 rules out standing emoji in for them in the meantime.
   */
  icon?: ReactNode;
  children?: ReactNode;
};

/**
 * Selectable pill, built for the Phase 6 mood chips.
 *
 * Selected state is carried by the clay wash plus a stronger rule and ink text
 * — not by the clay fill. A row of six filled clay chips would spend the
 * accent colour that §8 reserves for one deliberate use per view.
 */
export function Chip({
  selected = false,
  icon,
  className,
  children,
  type = "button",
  ...props
}: ChipProps) {
  return (
    <button
      type={type}
      aria-pressed={selected}
      className={cn(
        "inline-flex items-center gap-2 rounded-full",
        // 44px tall, but visually a pill: the padding does the shaping and the
        // height keeps the target legal.
        "h-11 px-4",
        "font-sans text-body-sm",
        "border transition-[background-color,border-color,color] duration-150 ease-out",
        selected
          ? "bg-clay-wash border-clay text-ink font-medium"
          : "bg-surface-raised border-ink-faint text-ink-soft enabled:hover:border-ink-soft enabled:hover:text-ink",
        focusRing,
        disabledControl,
        className,
      )}
      {...props}
    >
      {icon ? (
        <span aria-hidden="true" className="[&_svg]:h-4 [&_svg]:w-4">
          {icon}
        </span>
      ) : null}
      {children}
    </button>
  );
}
