"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { useSession } from "@/lib/auth/SessionProvider";
import { describeAuthError } from "@/lib/auth/errors";
import type { UserSummary } from "@/lib/auth/types";

/**
 * What the site knows about you, and what you can do about it.
 *
 * ===========================================================================
 * Why this calls /me when it already has the user
 * ===========================================================================
 *
 * `signIn` and every refresh return a `UserSummary` alongside the access token,
 * so this panel could render entirely from what is already in memory. It asks
 * the server anyway, once, on mount.
 *
 * Two reasons. The first is correctness: the copy in memory was taken at the
 * last refresh, which may have been fourteen minutes ago, and a role granted by
 * `APP_ADMIN_BOOTSTRAP_EMAILS` on a restart would not appear until then. The
 * access token deliberately carries no email or display name for exactly this
 * reason — `/me` is one query and always current.
 *
 * The second is that this is the only place in the application that sends a
 * Bearer token to an authenticated endpoint. If the token handling in
 * `SessionProvider` is wrong, this page is where it shows, rather than in phase
 * 7 on top of a feature.
 *
 * ===========================================================================
 * What is deliberately not here
 * ===========================================================================
 *
 * **No "your email is unverified" notice.** The column exists, `/me` reports
 * it, and nothing in the application depends on it — there is no mail
 * transport, so there is no verification to complete. A warning badge beside an
 * address, with no button that could resolve it, is a permanent complaint about
 * something the person cannot fix. It goes in when email does.
 *
 * **No delete-account button.** There is no endpoint, and a button that opened
 * a mailto would be pretending. The link to /contact says the true thing: a
 * person reads that inbox and will do it.
 *
 * **No password change.** Same reason as the missing "forgot password" on
 * /login: it needs email to be safe, and email does not exist yet.
 */
export function AccountPanel({ user: initialUser }: { user: UserSummary }) {
  const { authFetch, signOut } = useSession();
  const router = useRouter();
  const [user, setUser] = useState(initialUser);
  const [signingOut, setSigningOut] = useState(false);
  const [signOutError, setSignOutError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    void authFetch<UserSummary>("/api/v1/auth/me")
      .then((fresh) => {
        if (!cancelled) setUser(fresh);
      })
      .catch(() => {
        // Deliberately silent. The panel is already rendering a copy that came
        // from a successful sign-in moments ago, so there is nothing useful to
        // say and nothing for the person to do. A genuine 401 has already been
        // handled inside authFetch, which signs out and sends them to /login.
      });

    return () => {
      cancelled = true;
    };
  }, [authFetch]);

  return (
    <div className="mt-10 max-w-2xl">
      <Card tone="raised">
        <dl className="divide-y divide-rule">
          <Row label="Name">{user.displayName?.trim() || "Not set"}</Row>
          <Row label="Email">
            <span className="break-all">{user.email}</span>
          </Row>
          <Row label="Signed up">{formatDate(user.createdAt)}</Row>
          {/*
            Shown only to admins. Telling everyone else "Role: USER" invents a
            hierarchy the site does not otherwise have, on a page about them.
          */}
          {user.role === "ADMIN" ? <Row label="Role">Moderator</Row> : null}
        </dl>
      </Card>

      <div className="mt-8">
        <Button
          variant="secondary"
          loading={signingOut}
          loadingLabel="Signing out"
          onClick={() => {
            setSigningOut(true);
            setSignOutError(null);
            // Only navigate when the server actually revoked the family. On a
            // failure the session is still live, so this stays put, re-enables
            // the button and says so rather than showing a signed-out site.
            void signOut().then(
              () => router.push("/"),
              (error: unknown) => {
                setSigningOut(false);
                setSignOutError(describeAuthError(error));
              },
            );
          }}
        >
          Sign out
        </Button>
        {signOutError !== null ? (
          <p role="alert" className="mt-3 max-w-prose text-body-sm text-danger">
            You are still signed in — sign-out did not complete. {signOutError}
          </p>
        ) : null}
      </div>

      <p className="mt-10 max-w-prose text-body-sm text-ink-soft">
        Nothing you have ever written in the vent box is here, because none of it was ever
        sent to us. To have this account removed,{" "}
        <Link
          href="/contact"
          className={cn(
            "rounded-sm font-medium text-(--color-text-accent) underline underline-offset-2",
            "transition-colors duration-150 ease-out hover:text-clay-active",
            focusRing,
          )}
        >
          get in touch
        </Link>{" "}
        and a person will do it.
      </p>
    </div>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-1 py-4 first:pt-0 last:pb-0 sm:flex-row sm:gap-6">
      <dt className="font-sans text-body-sm font-medium text-ink sm:w-40 sm:shrink-0">
        {label}
      </dt>
      <dd className="text-body text-ink-soft">{children}</dd>
    </div>
  );
}

/**
 * The date only, in the reader's own locale.
 *
 * Formatted in an effect-free render on the client, which means the server and
 * the first client render could disagree if this were server-rendered — it is
 * not: the whole panel is behind `RequireAuth` and only ever renders in the
 * browser.
 */
function formatDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Unknown";
  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}
