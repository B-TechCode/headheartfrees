"use client";

import { PageHeader } from "@/components/sections/PageHeader";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { AccountPanel } from "@/components/auth/AccountPanel";

/*
 * /account — the phase's one protected route.
 *
 * A client component all the way down, unusually for this codebase, and there
 * is no server-rendered shell above `RequireAuth`. The heading is inside the
 * guard on purpose: a server-rendered "Your account" title would be sent to
 * everyone including the signed-out, who would see it for the moment before
 * being redirected, and a page titled after an account you do not have is a
 * confusing thing to flash at someone.
 *
 * The cost is that `export const metadata` is unavailable in a client
 * component, so the tab title falls back to the layout's default. That is the
 * right trade for a page that is `noindex` by nature and that nobody reaches
 * except by already being here.
 */
export default function AccountPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <RequireAuth>
        {(user) => (
          <>
            <PageHeader
              eyebrow="Account"
              title="Your account"
              lede="Everything the site holds about you is on this page. It is a short page by design."
            />
            <AccountPanel user={user} />
          </>
        )}
      </RequireAuth>
    </div>
  );
}
