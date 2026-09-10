"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { RatingInput } from "@/components/ui/RatingInput";
import { Textarea } from "@/components/ui/Textarea";
import { cn } from "@/lib/cn";
import { secondaryAction } from "@/components/ui/styles";
import { describeAuthError } from "@/lib/auth/errors";
import { fieldErrorsOf } from "@/lib/auth/errors";
import { useSession } from "@/lib/auth/SessionProvider";
import { submitFeedback } from "@/lib/feedback";

const MAX_MESSAGE = 2000;

/**
 * The feedback form.
 *
 * ===========================================================================
 * The two things this form has to say out loud
 * ===========================================================================
 *
 * **1. It is reviewed before publication.** People submit into forms expecting
 * either instant publication or a private inbox, and this is neither.
 *
 * **2. Nothing from the vent box is attached.** This is the one that matters.
 * The form appears seconds after someone wrote something private on the
 * previous screen, and the reasonable assumption is that the two are
 * connected — that this is the "tell us more" step. It is not, it never was,
 * and the words never left their browser. Saying so is not reassurance
 * boilerplate; it is the only place a person can find that out.
 *
 * The name and location fields carry their own notice, because a form field is
 * assumed private until told otherwise and these two are printed next to
 * somebody's words on a public page.
 *
 * ===========================================================================
 * Signed in
 * ===========================================================================
 *
 * The display name is prefilled from the session and is **editable and
 * clearable**. Being signed in must not force attribution: the prefill is a
 * convenience, and an empty field posts an anonymous note even from an
 * authenticated request. The backend records `user_id` either way, so a
 * removal request can still be honoured, but nothing published carries a name
 * the person did not leave there.
 *
 * The prefill happens here rather than on the server on purpose — it keeps the
 * `feedback` module free of any dependency on `auth` (PROJECT_BRIEF.md §4),
 * and it means a later rename of an account cannot rewrite a name already
 * published under the old one.
 */
export function FeedbackForm({
  onSubmitted,
  onCancel,
}: {
  onSubmitted: () => void;
  onCancel: () => void;
}) {
  const { status, user, authFetch } = useSession();

  const [rating, setRating] = useState<number | null>(null);
  const [message, setMessage] = useState("");
  const [displayName, setDisplayName] = useState(user?.displayName ?? "");
  const [location, setLocation] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setFormError(null);
    setFieldErrors({});

    if (rating === null) {
      // Caught before the request so the person is told which control is
      // missing, rather than being handed a server message about a field.
      setFieldErrors({ rating: "Please choose a rating from 1 to 5." });
      setSubmitting(false);
      return;
    }

    try {
      await submitFeedback(
        {
          rating,
          message: message.trim(),
          displayName: displayName.trim() || undefined,
          location: location.trim() || undefined,
        },
        // Signed in, the request carries a token and the row records who sent
        // it - which is what makes a later removal request answerable. It does
        // NOT decide whether a name is published; the cleared field does that.
        // Anonymous, this is undefined and the call is a plain apiFetch.
        status === "authenticated" ? authFetch : undefined,
      );
      onSubmitted();
    } catch (error) {
      setFormError(describeAuthError(error));
      setFieldErrors(fieldErrorsOf(error));
      setSubmitting(false);
    }
  }

  const remaining = MAX_MESSAGE - message.length;

  return (
    <form onSubmit={onSubmit} noValidate className="mt-6 flex max-w-xl flex-col gap-6">
      <p className="max-w-[58ch] text-body-sm text-ink-soft">
        Notes are read by a person before they appear anywhere. Nothing you wrote in the vent
        box is attached to this — we never received it.
      </p>

      {formError !== null ? (
        <Callout tone="caution" title="That did not send">
          <p>{formError}</p>
        </Callout>
      ) : null}

      <RatingInput value={rating} onChange={setRating} error={fieldErrors.rating} />

      <FormField
        label="Your note"
        error={fieldErrors.message}
        hint={`Anything you like, from a few words up to ${MAX_MESSAGE} characters.`}
      >
        {(ids) => (
          <>
            <Textarea
              {...ids}
              name="message"
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              maxLength={MAX_MESSAGE}
              rows={5}
              required
            />
            <p
              aria-live="polite"
              className={cn(
                "mt-1 text-right font-sans text-caption",
                remaining < 100 ? "text-danger" : "text-ink-soft",
              )}
            >
              {remaining} characters left
            </p>
          </>
        )}
      </FormField>

      {/*
        The published-ness notice sits with the two fields it applies to, not
        in the intro paragraph. Someone filling in a name field has stopped
        reading the preamble by then, and a form field is assumed private
        until it says otherwise. Moderation is the backstop, not the notice —
        but a person should not need the backstop to know what they agreed to.
      */}
      <fieldset className="min-w-0 border-0 p-0">
        <legend className="font-sans text-body-sm font-medium text-ink">
          If you want to be named
        </legend>
        <p className="mt-2 max-w-[58ch] text-body-sm text-ink-soft">
          Both of these are optional and both are published with your note. Leave them empty to
          post anonymously.
        </p>

        <div className="mt-4 flex flex-col gap-6 sm:flex-row">
          <FormField
            label="Name"
            optional
            error={fieldErrors.displayName}
            className="flex-1"
          >
            {(ids) => (
              <Input
                {...ids}
                name="displayName"
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                maxLength={60}
                autoComplete="off"
                placeholder="Ada"
              />
            )}
          </FormField>

          <FormField
            label="Where you are"
            optional
            error={fieldErrors.location}
            className="flex-1"
          >
            {(ids) => (
              <Input
                {...ids}
                name="location"
                value={location}
                onChange={(event) => setLocation(event.target.value)}
                maxLength={60}
                autoComplete="off"
                // The placeholder teaches the format rather than explaining
                // the rule: one place, no commas. A person who writes
                // "Mumbai, India" gets a specific message back.
                placeholder="Mumbai"
              />
            )}
          </FormField>
        </div>
      </fieldset>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-4">
        <Button type="submit" loading={submitting} loadingLabel="Sending">
          Send note
        </Button>

        <button
          type="button"
          onClick={onCancel}
          className={cn(
            "inline-flex min-h-11 items-center text-body",
            secondaryAction,
          )}
        >
          Never mind
        </button>
      </div>
    </form>
  );
}
