"use client";

import Link from "next/link";
import { PageHeader } from "@/components/sections/PageHeader";
import { ModerationQueue } from "@/components/sections/ModerationQueue";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";

/**
 * The moderation page's shell: sign-in gate, role gate, then the queue.
 *
 * Split from the route file so the route can stay a server component and keep
 * its `metadata` export, which a `"use client"` module cannot have.
 */
export function AdminFeedbackScreen() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <RequireAuth>
        {(user) =>
          user.role === "ADMIN" ? (
            <>
              <PageHeader
                eyebrow="Moderation"
                title="Submitted notes"
                lede="Nothing appears on Voices until it is approved here. Approvals can be reversed at any time."
              />
              <ModerationQueue />
            </>
          ) : (
            <NotAModerator />
          )
        }
      </RequireAuth>
    </div>
  );
}

/**
 * A signed-in person who is not an admin.
 *
 * Says the plain thing and stops. No "request access" link, because there is
 * no request to make: the only route to the ADMIN role is
 * APP_ADMIN_BOOTSTRAP_EMAILS applied at startup, and no endpoint anywhere
 * accepts a role. Offering a button that cannot work would be worse than this.
 */
function NotAModerator() {
  return (
    <>
      <PageHeader
        eyebrow="Moderation"
        title="This page is not for your account"
        lede="Only moderators can open the queue. Nothing is wrong with your account — it simply does not have that role."
      />
      <Link
        href="/"
        className={cn(
          "mt-8 inline-flex min-h-11 items-center rounded-sm text-body-lg",
          "text-(--color-text-accent) underline decoration-clay underline-offset-4",
          "transition-colors duration-150 ease-out hover:text-ink hover:decoration-ink-faint",
          focusRing,
        )}
      >
        Back to home
      </Link>
    </>
  );
}
