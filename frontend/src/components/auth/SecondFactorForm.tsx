"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { CodeInput } from "@/components/auth/CodeInput";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { describeAuthError } from "@/lib/auth/errors";

/**
 * Step two of signing in: the code.
 *
 * ===========================================================================
 * Why this is a step on /login and not a route of its own
 * ===========================================================================
 *
 * A `/login/code` route would have to carry the ticket somewhere the next
 * page could read it — a query parameter, `sessionStorage`, or a cookie. All
 * three put a live credential somewhere it outlives the moment: a query string
 * lands in browser history and in any proxy log on the way, and storage
 * survives the tab.
 *
 * Held here it lives in a `useState` in one component, and a reload loses it.
 * Losing it is the correct outcome — the person re-enters their password,
 * which they know, and gets a fresh ticket. There is nothing to clean up.
 *
 * ===========================================================================
 * The error copy is the server's
 * ===========================================================================
 *
 * Every failure of this step — wrong code, expired ticket, a code already
 * used, an unknown or spent backup code — returns one identical message from
 * the API. This component renders it and does not improve on it. A friendlier
 * "that code has expired" here would undo, in the layer nobody audits, the
 * property the backend went to some trouble to have: that an attacker cannot
 * learn which part of an attempt was wrong.
 */
export function SecondFactorForm({
  onSubmit,
  onCancel,
}: {
  /** @throws {ApiError} which this renders. Resolving means signed in. */
  onSubmit: (code: string) => Promise<void>;
  onCancel: () => void;
}) {
  const [code, setCode] = useState("");
  const [backupCode, setBackupCode] = useState("");
  const [usingBackupCode, setUsingBackupCode] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const submitted = usingBackupCode ? backupCode.trim() : code;
  const canSubmit = usingBackupCode ? submitted.length > 0 : submitted.length === 6;

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit) return;

    setSubmitting(true);
    setFormError(null);

    try {
      await onSubmit(submitted);
      // No navigation and no state reset here. Succeeding means the session
      // was adopted, and the effect on /login owns the redirect - leaving
      // `submitting` true keeps the button in its loading state through the
      // redirect rather than flicking back for a frame.
    } catch (error) {
      setFormError(describeAuthError(error));
      // Clear the field. The code that just failed is worthless either way:
      // if it was wrong it stays wrong, and if it was right it has now been
      // spent. Leaving it in place invites re-submitting the same six digits.
      setCode("");
      setBackupCode("");
      setSubmitting(false);
    }
  }

  return (
    <div className="mt-10 max-w-md">
      <p className="text-body text-ink-soft">
        Enter the six-digit code from your authenticator app.
      </p>

      <form onSubmit={handleSubmit} noValidate className="mt-8 flex flex-col gap-6">
        {usingBackupCode ? (
          <FormField
            label="Backup code"
            hint="One of the codes you saved when you set this up. Each one works once."
          >
            {(ids) => (
              <Input
                {...ids}
                type="text"
                name="backupCode"
                value={backupCode}
                onChange={(event) => setBackupCode(event.target.value)}
                autoComplete="off"
                autoCapitalize="characters"
                spellCheck={false}
                placeholder="XXXXX-XXXXX"
                className="max-w-[14rem] font-sans tracking-[0.12em] uppercase"
                autoFocus
                required
              />
            )}
          </FormField>
        ) : (
          <FormField label="Six-digit code">
            {(ids) => (
              <CodeInput
                {...ids}
                value={code}
                onChange={setCode}
                disabled={submitting}
                autoFocus
              />
            )}
          </FormField>
        )}

        {/*
          One live region for the form, above the button. A message under the
          submit control sits off the bottom of a short viewport with the
          keyboard open, which is exactly where this form is used.
        */}
        <p
          role="status"
          aria-live="polite"
          className={cn("text-body-sm text-danger", !formError && "sr-only")}
        >
          {formError ?? ""}
        </p>

        <Button
          type="submit"
          size="lg"
          loading={submitting}
          loadingLabel="Checking"
          disabled={!canSubmit}
        >
          Continue
        </Button>
      </form>

      <div className="mt-8 flex flex-col gap-3 text-body-sm text-ink-soft">
        <button
          type="button"
          onClick={() => {
            setUsingBackupCode((using) => !using);
            setFormError(null);
            setCode("");
            setBackupCode("");
          }}
          className={cn(
            "self-start rounded-sm font-medium text-(--color-text-accent) underline underline-offset-2",
            "transition-colors duration-150 ease-out hover:text-accent-active",
            focusRing,
          )}
        >
          {usingBackupCode ? "Use your authenticator app instead" : "Use a backup code instead"}
        </button>

        <button
          type="button"
          onClick={onCancel}
          className={cn(
            "self-start rounded-sm font-medium text-(--color-text-accent) underline underline-offset-2",
            "transition-colors duration-150 ease-out hover:text-accent-active",
            focusRing,
          )}
        >
          Start again
        </button>
      </div>
    </div>
  );
}
