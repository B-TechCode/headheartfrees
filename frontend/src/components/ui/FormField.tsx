"use client";

import { useId } from "react";
import type { ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Label } from "./Label";

export type FormFieldProps = {
  label: string;
  /**
   * Receives the ids to wire onto the control. Passing them through a render
   * prop is what makes the accessible relationship impossible to forget: there
   * is no way to render a field without being handed the `id` and
   * `aria-describedby` it needs.
   */
  children: (ids: {
    id: string;
    "aria-describedby": string | undefined;
    invalid: boolean;
  }) => ReactNode;
  /** Shown under the control, and announced. Presence switches on the error state. */
  error?: string;
  /** Persistent guidance. Stays visible when an error appears. */
  hint?: string;
  optional?: boolean;
  className?: string;
};

/**
 * Label + control + hint + error, with the ARIA wiring done once.
 *
 * The error is a live region so it is announced when it appears after a
 * submit attempt, rather than only when the field is next focused.
 */
export function FormField({
  label,
  children,
  error,
  hint,
  optional = false,
  className,
}: FormFieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-error`;

  const describedBy =
    [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(" ") || undefined;

  return (
    <div className={cn("flex flex-col gap-2", className)}>
      <Label htmlFor={id} optional={optional}>
        {label}
      </Label>

      {hint ? (
        <p id={hintId} className="text-body-sm text-ink-soft">
          {hint}
        </p>
      ) : null}

      {children({ id, "aria-describedby": describedBy, invalid: Boolean(error) })}

      {/*
        Always rendered so the region exists before the message does — an
        aria-live region added to the DOM at the same moment as its content is
        unreliably announced.
      */}
      <p
        id={errorId}
        role="status"
        aria-live="polite"
        className={cn("text-body-sm text-danger", !error && "sr-only")}
      >
        {error ?? ""}
      </p>
    </div>
  );
}
