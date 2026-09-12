import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Spinner } from "./Spinner";
import { disabledControl, focusRing } from "./styles";

export type ButtonVariant = "primary" | "secondary" | "ghost" | "destructive";
export type ButtonSize = "sm" | "md" | "lg";

/**
 * Depth comes from colour and rule weight, not from drop shadows — see
 * PROJECT_BRIEF.md §8, which names repeated shadows as a default-toolkit tell.
 * The only movement is a half-pixel press, which reads as tactile without
 * announcing itself.
 */
const variantClasses: Record<ButtonVariant, string> = {
  primary: cn(
    "bg-accent text-on-accent border border-transparent",
    "hover:bg-accent-hover active:bg-accent-active",
  ),
  secondary: cn(
    "bg-surface-raised text-ink border border-ink-faint",
    "hover:bg-surface-sunk active:bg-surface-sunk active:border-ink-faint",
  ),
  ghost: cn(
    "bg-transparent text-ink border border-transparent",
    "hover:bg-accent-wash active:bg-accent-wash active:border-accent-wash",
  ),
  destructive: cn(
    "bg-danger text-on-danger border border-transparent",
    "hover:bg-danger-hover active:bg-danger-active",
  ),
};

/** Every size clears 44px, so `sm` is a narrower button and not a smaller target. */
const sizeClasses: Record<ButtonSize, string> = {
  sm: "h-11 px-4 text-body-sm gap-2",
  md: "h-12 px-5 text-body gap-2.5",
  lg: "h-14 px-7 text-body-lg gap-3",
};

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /**
   * Shows a spinner and blocks interaction. The label stays visible rather than
   * being swapped out, so the button does not change width mid-action and the
   * user does not lose the thing they were about to press.
   */
  loading?: boolean;
  /** Announced while `loading`. */
  loadingLabel?: string;
  fullWidth?: boolean;
  children?: ReactNode;
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  loadingLabel = "Working",
  fullWidth = false,
  disabled,
  className,
  children,
  type = "button",
  ...props
}: ButtonProps) {
  const isDisabled = disabled === true || loading;

  return (
    <button
      type={type}
      disabled={isDisabled}
      aria-busy={loading || undefined}
      className={cn(
        "inline-flex items-center justify-center rounded-md",
        "font-sans font-medium tracking-[0.005em] whitespace-nowrap",
        "transition-[background-color,border-color,color] duration-150 ease-out",
        "active:translate-y-[0.5px]",
        sizeClasses[size],
        variantClasses[variant],
        focusRing,
        disabledControl,
        fullWidth && "w-full",
        className,
      )}
      {...props}
    >
      {loading ? <Spinner label={loadingLabel} /> : null}
      {children}
    </button>
  );
}
