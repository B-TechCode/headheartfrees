"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { Card } from "@/components/ui/Card";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { CodeInput } from "@/components/auth/CodeInput";
import { TotpSetup } from "@/components/auth/TotpSetup";
import { BackupCodes } from "@/components/auth/BackupCodes";
import { useSession } from "@/lib/auth/SessionProvider";
import { describeAuthError } from "@/lib/auth/errors";
import { cn } from "@/lib/cn";
import type { BackupCodesResponse, UserSummary } from "@/lib/auth/types";

/**
 * Two-step sign-in, on the account page.
 *
 * Four states, and which one shows is decided entirely by the account:
 *
 * - **Off, optional** (a USER who has not enrolled) — an offer, phrased as one.
 * - **Setting up** — the QR, the key, the verification.
 * - **Showing codes** — after enrolling or regenerating. Shown once.
 * - **On** — the remaining-code count, a way to get more, and for a USER a way
 *   to turn it off. An ADMIN gets no off switch, because the server refuses
 *   and an enabled control that always errors is worse than no control.
 */
type Mode =
  | { name: "idle" }
  | { name: "setting-up" }
  | { name: "codes"; codes: string[] }
  | { name: "regenerating" }
  | { name: "disabling" };

export function TwoStepCard({
  user,
  onChanged,
}: {
  user: UserSummary;
  /** Re-reads `/me`, so the count and the on/off state stay honest. */
  onChanged: () => void;
}) {
  const { authFetch } = useSession();
  const [mode, setMode] = useState<Mode>({ name: "idle" });

  if (mode.name === "codes") {
    return (
      <Section>
        <BackupCodes
          codes={mode.codes}
          onAcknowledge={() => {
            setMode({ name: "idle" });
            onChanged();
          }}
        />
      </Section>
    );
  }

  if (mode.name === "setting-up") {
    return (
      <Section>
        <TotpSetup
          call={authFetch}
          ticket={null}
          onEnrolled={(codes) => setMode({ name: "codes", codes })}
          onCancel={() => setMode({ name: "idle" })}
        />
      </Section>
    );
  }

  if (mode.name === "regenerating") {
    return (
      <Section>
        <CodeGatedAction
          title="New backup codes"
          description="This replaces all of your existing codes. The old ones stop working straight away."
          submitLabel="Generate new codes"
          onSubmit={async (code) => {
            const response = await authFetch<BackupCodesResponse>(
              "/api/v1/auth/totp/backup-codes",
              { method: "POST", body: { code }, abortable: false },
            );
            setMode({ name: "codes", codes: response.backupCodes });
          }}
          onCancel={() => setMode({ name: "idle" })}
        />
      </Section>
    );
  }

  if (mode.name === "disabling") {
    return (
      <Section>
        <CodeGatedAction
          title="Turn off two-step sign-in"
          description="Your password alone will sign you in again. This signs you out everywhere, including on this device."
          submitLabel="Turn it off"
          destructive
          withPassword
          onSubmit={async (code, password) => {
            await authFetch<void>("/api/v1/auth/totp/disable", {
              method: "POST",
              body: { password, code },
              abortable: false,
            });
            setMode({ name: "idle" });
            onChanged();
          }}
          onCancel={() => setMode({ name: "idle" })}
        />
      </Section>
    );
  }

  if (!user.totpEnabled) {
    return (
      <Section>
        <p className="text-body text-ink-soft">
          {user.totpRequired
            ? "Your account needs a code from an authenticator app as well as a password. It is not set up yet."
            : "You can add a six-digit code from an authenticator app on top of your password. Optional, and it takes about a minute."}
        </p>
        <div className="mt-6">
          <Button onClick={() => setMode({ name: "setting-up" })}>Set it up</Button>
        </div>
      </Section>
    );
  }

  return (
    <Section>
      <p className="text-body text-ink-soft">
        On. Signing in asks for a code from your authenticator app after your password.
      </p>

      {/*
        The count, and a nudge when it is getting thin. Somebody who has used
        eight of ten codes has two left and no reason to have noticed - this is
        the only place that would ever tell them.
      */}
      {user.backupCodesRemaining <= 3 ? (
        <Callout tone="caution" className="mt-6">
          <p>
            {user.backupCodesRemaining === 0
              ? "You have no backup codes left. If you lose your phone there is no way back in without help."
              : `Only ${user.backupCodesRemaining} backup ${
                  user.backupCodesRemaining === 1 ? "code" : "codes"
                } left. Generate a new set while your authenticator still works.`}
          </p>
        </Callout>
      ) : (
        <p className="mt-3 text-body-sm text-ink-soft">
          {user.backupCodesRemaining} backup codes left.
        </p>
      )}

      <div className="mt-6 flex flex-wrap gap-4">
        <Button variant="secondary" onClick={() => setMode({ name: "regenerating" })}>
          New backup codes
        </Button>
        {/*
          No off switch for an admin. The server refuses it (403), so rendering
          the button would be offering something that cannot happen - and the
          only thing it could achieve is forced re-enrolment at the next
          sign-in, arrived at by surprise.
        */}
        {user.totpRequired ? null : (
          <Button variant="ghost" onClick={() => setMode({ name: "disabling" })}>
            Turn it off
          </Button>
        )}
      </div>

      {user.totpRequired ? (
        <p className="mt-4 text-body-sm text-ink-soft">
          This cannot be turned off on an admin account.
        </p>
      ) : null}
    </Section>
  );
}

function Section({ children }: { children: React.ReactNode }) {
  return (
    <Card tone="raised" className="mt-8">
      <h2 className="font-display text-h4 text-ink">Two-step sign-in</h2>
      <div className="mt-4">{children}</div>
    </Card>
  );
}

/**
 * An action that needs a current code, and sometimes the password too.
 *
 * Both regeneration and disabling are "prove you are still the person holding
 * the authenticator" — one form, used twice, so the two cannot drift apart in
 * how strictly they ask.
 */
function CodeGatedAction({
  title,
  description,
  submitLabel,
  destructive = false,
  withPassword = false,
  onSubmit,
  onCancel,
}: {
  title: string;
  description: string;
  submitLabel: string;
  destructive?: boolean;
  withPassword?: boolean;
  onSubmit: (code: string, password: string) => Promise<void>;
  onCancel: () => void;
}) {
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const ready = code.length === 6 && (!withPassword || password.length > 0);

  return (
    <form
      noValidate
      className="flex flex-col gap-6"
      onSubmit={(event) => {
        event.preventDefault();
        if (!ready) return;
        setSubmitting(true);
        setError(null);
        void onSubmit(code, password).catch((problem: unknown) => {
          setError(describeAuthError(problem));
          setCode("");
          setSubmitting(false);
        });
      }}
    >
      <div>
        <h3 className="font-display text-h4 text-ink">{title}</h3>
        <p className="mt-2 text-body-sm text-ink-soft">{description}</p>
      </div>

      {withPassword ? (
        <FormField label="Password">
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
      ) : null}

      <FormField label="Six-digit code">
        {(ids) => (
          <CodeInput {...ids} value={code} onChange={setCode} disabled={submitting} autoFocus />
        )}
      </FormField>

      <p
        role="status"
        aria-live="polite"
        className={cn("text-body-sm text-danger", !error && "sr-only")}
      >
        {error ?? ""}
      </p>

      <div className="flex flex-wrap items-center gap-4">
        <Button
          type="submit"
          variant={destructive ? "destructive" : "primary"}
          loading={submitting}
          loadingLabel="Checking"
          disabled={!ready}
        >
          {submitLabel}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </Button>
      </div>
    </form>
  );
}
