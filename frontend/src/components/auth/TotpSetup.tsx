"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { FormField } from "@/components/ui/FormField";
import { Spinner } from "@/components/ui/Spinner";
import { CodeInput } from "@/components/auth/CodeInput";
import type { ApiRequestOptions } from "@/lib/api";
import { describeAuthError } from "@/lib/auth/errors";
import { cn } from "@/lib/cn";
import type { AccessTokenResponse, TotpEnableResponse, TotpSetupResponse } from "@/lib/auth/types";

/**
 * The enrolment screen: a QR code, the same secret as text, and a code to
 * prove the authenticator actually took it.
 *
 * ===========================================================================
 * Two callers, one component
 * ===========================================================================
 *
 * - **From `/account`**, by somebody already signed in who is turning the
 *   feature on. `call` is the session's `authFetch` and `ticket` is null.
 * - **From `/login`**, by an admin who was stopped at sign-in and sent here
 *   instead of being given a session. `call` is the plain `apiFetch` and
 *   `ticket` is the enrolment ticket that stands in for the session they do
 *   not have.
 *
 * The difference is entirely in those two props. Everything on screen is the
 * same, because it is the same job.
 *
 * ===========================================================================
 * Why the QR comes from the server as a data: URI
 * ===========================================================================
 *
 * It arrives already rendered, as `data:image/svg+xml;base64,...`, and goes
 * straight into an `<img>`. No QR library in the bundle, no
 * `dangerouslySetInnerHTML`, and no new CSP allowance — `img-src 'self' data:`
 * has been in the policy since phase 9 for the paper-grain overlay.
 *
 * A `<img>` is also an isolated, script-free context, so the SVG never becomes
 * live DOM in this document.
 */
export function TotpSetup({
  call,
  ticket,
  onEnrolled,
  onCancel,
}: {
  /** `authFetch` when signed in, `apiFetch` when holding an enrolment ticket. */
  call: <T>(path: string, options?: ApiRequestOptions) => Promise<T>;
  ticket: string | null;
  /**
   * Called once enrolment succeeds, with whatever the server returned.
   *
   * `session` is present only on the ticket path. The caller decides what to
   * do with it — this component does not touch the session, because on the
   * `/account` path there is already one and adopting a second would be wrong.
   */
  onEnrolled: (backupCodes: string[], session?: AccessTokenResponse) => void;
  onCancel?: () => void;
}) {
  const [setup, setSetup] = useState<TotpSetupResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [code, setCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    void call<TotpSetupResponse>("/api/v1/auth/totp/setup", {
      method: "POST",
      body: ticket === null ? {} : { ticket },
    })
      .then((response) => {
        if (!cancelled) setSetup(response);
      })
      .catch((error: unknown) => {
        if (!cancelled) setLoadError(describeAuthError(error));
      });

    return () => {
      cancelled = true;
    };
  }, [call, ticket]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (code.length !== 6) return;

    setSubmitting(true);
    setFormError(null);

    try {
      // abortable: false. This is the call that commits the enrolment AND
      // returns the backup codes — the only time they exist outside the
      // database. Cut short after the server committed, the account has a
      // second factor whose recovery codes nobody has ever seen.
      const response = await call<TotpEnableResponse>("/api/v1/auth/totp/enable", {
        method: "POST",
        body: ticket === null ? { code } : { ticket, code },
        abortable: false,
      });
      onEnrolled(response.backupCodes, response.session);
    } catch (error) {
      setFormError(describeAuthError(error));
      // The code is spent or wrong either way; clearing it stops somebody
      // resubmitting the same six digits and getting the same refusal.
      setCode("");
      setSubmitting(false);
    }
  }

  if (loadError !== null) {
    return (
      <Callout tone="caution" title="We could not start the setup">
        <p>{loadError}</p>
      </Callout>
    );
  }

  if (setup === null) {
    return (
      <div className="flex items-center gap-3 text-body-sm text-ink-soft">
        <Spinner />
        <span>Preparing your setup code…</span>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-8">
      <ol className="flex flex-col gap-8">
        <li className="flex flex-col gap-3">
          <h3 className="font-display text-h4 text-ink">1. Scan this with an authenticator app</h3>
          <p className="text-body-sm text-ink-soft">
            Google Authenticator, 1Password, Bitwarden, Aegis — any of them. The app then shows a
            six-digit code that changes every thirty seconds.
          </p>
          {/*
            Explicit white ground behind the code and a fixed size. The image is
            read by a camera, not by a person, so it must not inherit the page
            theme: a dark-mode inversion of a QR code does not scan on many
            phones.
          */}
          <div className="mt-1 w-fit rounded-lg bg-white p-3">
            {/*
              A plain <img>, and next/image is the wrong tool here rather than
              an oversight. The source is a data: URI generated per request:
              there is no URL for a loader to optimise, no remote host to
              configure, and nothing to cache. Routing it through next/image
              would re-encode a few hundred bytes of SVG through an image
              pipeline for no benefit, and on a self-hosted deployment that
              pipeline is a server-side cost per enrolment.
            */}
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={setup.qrDataUri}
              alt=""
              width={188}
              height={188}
              className="block h-[188px] w-[188px]"
            />
          </div>
          {/*
            alt="" above, deliberately. The image carries no information a
            screen reader user can act on — it is a picture of a string, and
            that string is written out in full below as selectable text. An alt
            describing it ("QR code") announces a dead end; the manual key is
            the accessible path and it is not a fallback, it is the same secret.
          */}
        </li>

        <li className="flex flex-col gap-3">
          <h3 className="font-display text-h4 text-ink">2. Or type the key in by hand</h3>
          <p className="text-body-sm text-ink-soft">
            If the camera will not cooperate, or you are reading this on the same phone that holds
            the app.
          </p>
          <code
            className={cn(
              "mt-1 block w-fit rounded-md border border-rule bg-surface-sunk px-4 py-3",
              "font-sans text-body tracking-[0.18em] break-all text-ink select-all",
            )}
          >
            {setup.manualKey}
          </code>
        </li>

        <li className="flex flex-col gap-3">
          <h3 className="font-display text-h4 text-ink">3. Enter the code it shows</h3>
          <p className="text-body-sm text-ink-soft">
            This is the step that turns it on. We will not enable anything until a code from your
            app has actually worked — otherwise a mistyped key would lock you out at your next
            sign-in.
          </p>

          <form onSubmit={handleSubmit} noValidate className="mt-2 flex flex-col gap-6">
            <FormField label="Six-digit code">
              {(ids) => (
                <CodeInput
                  {...ids}
                  value={code}
                  onChange={setCode}
                  disabled={submitting}
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

            <div className="flex flex-wrap items-center gap-4">
              <Button
                type="submit"
                size="lg"
                loading={submitting}
                loadingLabel="Checking"
                disabled={code.length !== 6}
              >
                Turn on two-step sign-in
              </Button>
              {onCancel ? (
                <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
                  Cancel
                </Button>
              ) : null}
            </div>
          </form>
        </li>
      </ol>
    </div>
  );
}
