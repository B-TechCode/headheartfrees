"use client";

import { useState } from "react";
import { Callout } from "@/components/ui/Callout";
import { PageHeader } from "@/components/sections/PageHeader";
import { TotpSetup } from "@/components/auth/TotpSetup";
import { BackupCodes } from "@/components/auth/BackupCodes";
import { apiFetch } from "@/lib/api";
import { useSession } from "@/lib/auth/SessionProvider";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import type { AccessTokenResponse } from "@/lib/auth/types";

/**
 * Enrolment, standing in the middle of a sign-in.
 *
 * This is what an admin sees when their password was correct and their account
 * has no second factor yet — on the day this shipped, and afterwards for any
 * account newly promoted with `APP_ADMIN_BOOTSTRAP_EMAILS`.
 *
 * ===========================================================================
 * The sequence matters, and it is not "enrol, then sign in again"
 * ===========================================================================
 *
 * `/auth/totp/enable` returns the session alongside the backup codes when it
 * was reached on an enrolment ticket. So the last thing the person does is
 * read their codes, and pressing the button under them lands them where they
 * were going. Making them re-enter a password after enrolling would be a third
 * form in a flow that already has two, and every extra step here is one more
 * chance to close the tab on codes that cannot be shown again.
 *
 * That is also why the session is adopted **after** the codes are
 * acknowledged, not when it arrives: adopting immediately would let the
 * redirect on `/login` fire and replace this screen with the destination page,
 * taking the only copy of the recovery codes with it.
 */
export function TotpEnrolmentStep({
  ticket,
  onCancel,
}: {
  ticket: string;
  onCancel: () => void;
}) {
  const { adoptSession } = useSession();
  const [result, setResult] = useState<{
    backupCodes: string[];
    session?: AccessTokenResponse;
  } | null>(null);

  if (result !== null) {
    return (
      <div className="mt-10 max-w-2xl">
        <PageHeader
          eyebrow="Two-step sign-in"
          title="You are set up"
          lede="One last thing, and it is the part people skip."
        />
        <div className="mt-10">
          <BackupCodes
            codes={result.backupCodes}
            acknowledgeLabel="I have saved these — continue"
            onAcknowledge={() => {
              // Adopting the session is what lets /login's redirect effect
              // fire. Doing it here rather than on arrival is deliberate: see
              // the note at the top of this file.
              if (result.session) adoptSession(result.session);
            }}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="mt-10 max-w-2xl">
      <PageHeader
        eyebrow="Two-step sign-in"
        title="Set this up to continue"
        lede="Admin accounts need a code from an authenticator app as well as a password."
      />

      <Callout tone="note" className="mt-8">
        <p>
          This account can read and publish what people submit, so it is the one account on this
          site where a password on its own is not enough. It takes about a minute and you only do
          it once.
        </p>
      </Callout>

      <div className="mt-10">
        <TotpSetup
          // Plain apiFetch, not authFetch: there is no session yet. The
          // enrolment ticket in the body is the credential, and it is good for
          // these two endpoints and nothing else.
          call={apiFetch}
          ticket={ticket}
          onEnrolled={(backupCodes, session) => setResult({ backupCodes, session })}
        />
      </div>

      <p className="mt-10 text-body-sm text-ink-soft">
        Not the account you meant?{" "}
        <button
          type="button"
          onClick={onCancel}
          className={cn(
            "rounded-sm font-medium text-(--color-text-accent) underline underline-offset-2",
            "transition-colors duration-150 ease-out hover:text-clay-active",
            focusRing,
          )}
        >
          Start again
        </button>
      </p>
    </div>
  );
}
