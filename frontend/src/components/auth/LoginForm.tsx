"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { GoogleButton } from "@/components/auth/GoogleButton";
import { useSession } from "@/lib/auth/SessionProvider";
import { describeAuthError, fieldErrorsOf } from "@/lib/auth/errors";
import { GOOGLE_SIGN_IN_ENABLED, describeCallbackError } from "@/lib/auth/google";
import { sanitiseReturnTo } from "@/lib/auth/return-to";

/**
 * Sign in.
 *
 * There is no "forgot password" link, and its absence is deliberate rather
 * than an oversight. There is no mail transport in this application at all, so
 * a reset link would have nowhere to send anything; a link that opens a page
 * saying "not available yet" is worse than no link, because someone locked out
 * would follow it and then have to work out for themselves that there is no
 * way back. HANDOVER §10.1 records this as a known limitation and it is the
 * first thing email would buy.
 */
export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { status, signIn } = useSession();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  // Where to go afterwards. Sanitised on the way in: this arrives in a URL a
  // person can edit, and `//evil.example` starts with a slash too.
  const destination = sanitiseReturnTo(searchParams.get("next")) ?? "/";

  // Set by /auth/callback when Google came back with a refusal, so the
  // explanation lands on the page that offers a way forward rather than on a
  // page whose only job is to redirect.
  const callbackError = describeCallbackError(searchParams.get("error"));

  // The only place this page navigates away.
  //
  // It covers both cases with one rule: someone who was already signed in when
  // they arrived - a bookmark, or a tab left open while they signed in
  // elsewhere - and someone who has just succeeded below. Leaving the redirect
  // in the submit handler as well would fire it twice for the second case.
  //
  // `replace`, not `push`: /login should not be a stop on the back button once
  // it no longer applies.
  useEffect(() => {
    if (status === "authenticated") {
      router.replace(destination);
    }
  }, [status, destination, router]);

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});

    try {
      await signIn(email, password);
      // No navigation here - the effect above owns it. `submitting` is left
      // true on purpose, so the button stays in its loading state through the
      // redirect rather than flicking back to "Sign in" for a frame.
    } catch (error) {
      setFormError(describeAuthError(error));
      setFieldErrors(fieldErrorsOf(error));
      setSubmitting(false);
    }
  }

  return (
    <div className="mt-10 max-w-md">
      {callbackError !== null ? (
        <Callout tone="caution" title="Google sign-in did not finish" className="mb-8">
          <p>{callbackError}</p>
        </Callout>
      ) : null}

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

        <FormField label="Password" error={fieldErrors.password}>
          {(ids) => (
            <Input
              {...ids}
              type="password"
              name="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
              required
            />
          )}
        </FormField>

        {/*
          One live region for the whole form, above the button rather than
          below it: a message under the submit control is off the bottom of a
          short viewport with the keyboard open, which is where this form is
          most often used.
        */}
        <p role="status" aria-live="polite" className={cn("text-body-sm text-danger", !formError && "sr-only")}>
          {formError ?? ""}
        </p>

        <Button type="submit" size="lg" loading={submitting} loadingLabel="Signing in">
          Sign in
        </Button>
      </form>

      <p className="mt-8 text-body-sm text-ink-soft">
        No account?{" "}
        <Link
          href={destination === "/" ? "/register" : `/register?next=${encodeURIComponent(destination)}`}
          className={cn(
            "rounded-sm font-medium text-(--color-text-accent) underline underline-offset-2",
            "transition-colors duration-150 ease-out hover:text-clay-active",
            focusRing,
          )}
        >
          Create one
        </Link>
        . You do not need one to vent.
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
