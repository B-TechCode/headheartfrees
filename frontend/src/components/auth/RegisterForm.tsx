"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { GoogleButton } from "@/components/auth/GoogleButton";
import { ApiError, apiFetch } from "@/lib/api";
import { describeAuthError, fieldErrorsOf } from "@/lib/auth/errors";
import { GOOGLE_SIGN_IN_ENABLED } from "@/lib/auth/google";
import { sanitiseReturnTo } from "@/lib/auth/return-to";
import { signInHref } from "@/lib/auth/sign-in-href";
import type { RegistrationResponse } from "@/lib/auth/types";

/** Mirrors `PasswordPolicy.MIN_LENGTH`. Checked here for a faster answer, never instead. */
const MIN_PASSWORD_LENGTH = 12;

/**
 * Create an account.
 *
 * ===========================================================================
 * The success screen is the point of this component
 * ===========================================================================
 *
 * Registration returns the same 201 and the same body whether the address was
 * new or already had an account. That is deliberate and it is not negotiable:
 * a distinguishable response would let anyone use this site to test whether a
 * particular person has an account here, and on a mental health site that fact
 * is disclosive on its own.
 *
 * It has one real cost. Someone who registered months ago, has forgotten, and
 * registers again is told it worked — and if the screen stopped there, they
 * would go looking for an account that was never created and a password that
 * does not apply.
 *
 * The message the API returns is written to be true in both cases:
 *
 *   "If that address is new, your account is ready.
 *    If you already had one, sign in instead."
 *
 * **Render that string as it arrives, and keep the link to /login beside it.**
 * Do not replace it with a congratulatory one, and do not treat it as a
 * placeholder to improve. It is `RegistrationResponse.SHARED_MESSAGE` on the
 * backend, it is the agreed mitigation recorded in the phase 5 log, and
 * without a mail transport there is no better answer available.
 */
export function RegisterForm() {
  const searchParams = useSearchParams();
  const destination = sanitiseReturnTo(searchParams.get("next")) ?? "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [outcome, setOutcome] = useState<string | null>(null);

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});

    try {
      const response = await apiFetch<RegistrationResponse>("/api/v1/auth/register", {
        method: "POST",
        body: {
          email,
          password,
          // The column is nullable and the field is optional; an empty string
          // would store a display name that is a display name of nothing.
          displayName: displayName.trim() === "" ? null : displayName.trim(),
        },
      });
      setOutcome(response.message);
    } catch (error) {
      // A weak password comes back as a 400 with a message and no fieldErrors,
      // because the rule that rejected it is one sentence rather than a bean
      // validation failure. Putting it under the password field is where a
      // person will look for it.
      if (error instanceof ApiError && error.code === "WEAK_PASSWORD") {
        setFieldErrors({ password: error.message });
      } else {
        setFormError(describeAuthError(error));
        setFieldErrors(fieldErrorsOf(error));
      }
      setSubmitting(false);
    }
  }

  if (outcome !== null) {
    return (
      <div className="mt-10 max-w-md">
        <Callout tone="important" title="Check what happens next">
          {/* The API's words, not ours. See the comment above. */}
          <p>{outcome}</p>
          <p className="mt-5">
            <Link
              href={signInHref(destination === "/" ? null : destination)}
              className={cn(
                "rounded-sm font-medium text-(--color-text-accent) underline underline-offset-2",
                "transition-colors duration-150 ease-out hover:text-accent-active",
                focusRing,
              )}
            >
              Go to sign in
            </Link>
          </p>
        </Callout>

        <p className="mt-8 text-body-sm text-ink-soft">
          Nothing about venting changes either way — it never needed an account and still
          does not.
        </p>
      </div>
    );
  }

  return (
    <div className="mt-10 max-w-md">
      {GOOGLE_SIGN_IN_ENABLED ? (
        <>
          <GoogleButton returnTo={destination} />
          <Divider />
        </>
      ) : null}

      <form onSubmit={onSubmit} noValidate className="flex flex-col gap-6">
        <FormField label="Email" error={fieldErrors.email}>
          {(ids) => (
            <Input
              {...ids}
              type="email"
              name="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              autoCapitalize="none"
              spellCheck={false}
              required
            />
          )}
        </FormField>

        <FormField
          label="Password"
          error={fieldErrors.password}
          hint={`At least ${MIN_PASSWORD_LENGTH} characters. A phrase you will remember beats a short jumble you will not.`}
        >
          {(ids) => (
            <Input
              {...ids}
              type="password"
              name="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="new-password"
              minLength={MIN_PASSWORD_LENGTH}
              required
            />
          )}
        </FormField>

        <FormField
          label="What should we call you?"
          optional
          error={fieldErrors.displayName}
          hint="Shown to you, and on any feedback you choose to publish. Not your real name unless you want it to be."
        >
          {(ids) => (
            <Input
              {...ids}
              type="text"
              name="displayName"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              autoComplete="nickname"
              maxLength={80}
            />
          )}
        </FormField>

        <p
          role="status"
          aria-live="polite"
          className={cn("text-body-sm text-danger", !formError && "sr-only")}
        >
          {formError ?? ""}
        </p>

        <Button type="submit" size="lg" loading={submitting} loadingLabel="Creating your account">
          Create account
        </Button>
      </form>

      <p className="mt-8 text-body-sm text-ink-soft">
        Already have one?{" "}
        <Link
          href={signInHref(destination === "/" ? null : destination)}
          className={cn(
            "rounded-sm font-medium text-(--color-text-accent) underline underline-offset-2",
            "transition-colors duration-150 ease-out hover:text-accent-active",
            focusRing,
          )}
        >
          Sign in
        </Link>
        .
      </p>
    </div>
  );
}

function Divider() {
  return (
    <div className="my-7 flex items-center gap-4" aria-hidden="true">
      <span className="h-px flex-1 bg-rule" />
      <span className="font-sans text-caption text-ink-soft">or</span>
      <span className="h-px flex-1 bg-rule" />
    </div>
  );
}
