"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useCallback, useEffect, useId, useRef, useState } from "react";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { useSession } from "@/lib/auth/SessionProvider";
import { describeAuthError } from "@/lib/auth/errors";
import type { UserSummary } from "@/lib/auth/types";

/**
 * The avatar menu, hand-rolled.
 *
 * Phase 2 deferred "Radix, for the avatar dropdown only" to this phase. It is
 * still not installed: every other primitive in `components/ui` is hand-rolled,
 * the navbar already hand-rolls a disclosure with the same escape-and-restore
 * behaviour, and a dependency that exists to render one menu on one page is not
 * a trade this project has been making. What Radix would have brought — roving
 * focus, `role="menu"` semantics, outside-click and escape handling — is below
 * and is about fifty lines. If a second or third menu ever appears, that is the
 * moment to reconsider, not now.
 *
 * There is no avatar image. Nobody uploads one, Google's `picture` claim is
 * deliberately not stored, and a `<img>` pointing at `googleusercontent.com`
 * would tell Google which pages of this site a person is looking at — which is
 * not a thing to do on a mental health site to save drawing a circle.
 */
export function AccountMenu({ user }: { user: UserSummary }) {
  const { signOut } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  const menuId = useId();

  const [open, setOpen] = useState(false);
  const [signingOut, setSigningOut] = useState(false);
  const [signOutError, setSignOutError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  const close = useCallback((returnFocus: boolean) => {
    setOpen(false);
    if (returnFocus) {
      triggerRef.current?.focus();
    }
  }, []);

  // Navigating away closes it, exactly as the mobile disclosure does. Without
  // this the panel hangs over the page the person just moved to.
  useEffect(() => {
    setOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!open) return;

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        close(true);
      }
    }

    // `pointerdown`, not `click`: a click that lands on another control should
    // close this and let that control work, and waiting for `click` means the
    // menu is still open while the other thing is being pressed.
    function onPointerDown(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) {
        close(false);
      }
    }

    document.addEventListener("keydown", onKeyDown);
    document.addEventListener("pointerdown", onPointerDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.removeEventListener("pointerdown", onPointerDown);
    };
  }, [open, close]);

  /**
   * The menu's items, read from the DOM rather than collected into a ref array.
   *
   * The array version needs an index per item, and the indices shift when the
   * ADMIN entry appears or disappears — which leaves a stale detached node at
   * whichever index the list used to be longer by, and arrow keys then move
   * focus to nothing. Asking the DOM cannot get out of step with what is
   * rendered, and there are at most three items to find.
   */
  const menuItems = useCallback(
    () =>
      Array.from(
        containerRef.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [],
      ),
    [],
  );

  // Opening moves focus into the menu, which is what makes it operable from the
  // keyboard at all — a `role="menu"` nobody can reach is worse than a plain
  // list, because it also tells a screen reader to expect behaviour it will
  // not get.
  useEffect(() => {
    if (open) {
      menuItems()[0]?.focus();
    }
  }, [open, menuItems]);

  const onItemKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      if (event.key === "Tab") {
        // Tabbing out of a menu closes it rather than leaving a panel open
        // behind the focus ring.
        setOpen(false);
        return;
      }
      if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;

      event.preventDefault();
      const items = menuItems();
      if (items.length === 0) return;

      const from = items.indexOf(document.activeElement as HTMLElement);
      const delta = event.key === "ArrowDown" ? 1 : -1;
      // Wraps, so the last item's ArrowDown returns to the first.
      const next = (Math.max(from, 0) + delta + items.length) % items.length;
      items[next]?.focus();
    },
    [menuItems],
  );

  const onSignOut = useCallback(async () => {
    setSigningOut(true);
    setSignOutError(null);
    try {
      await signOut();
    } catch (error) {
      // The session is still live on the server, so the menu stays open, the
      // avatar stays, and the person is told. Navigating to "/" here would
      // show a signed-out header over a session that still exists.
      setSigningOut(false);
      setSignOutError(describeAuthError(error));
      return;
    }
    setOpen(false);
    // Home rather than staying put: the current page may be one that requires
    // an account, and RequireAuth would otherwise bounce them to /login with a
    // return destination they just chose to leave.
    router.push("/");
  }, [signOut, router]);

  return (
    <div ref={containerRef} className="relative">
      <button
        ref={triggerRef}
        type="button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={menuId}
        onClick={() => setOpen((wasOpen) => !wasOpen)}
        onKeyDown={(event) => {
          if (event.key === "ArrowDown") {
            event.preventDefault();
            setOpen(true);
          }
        }}
        className={cn(
          "inline-flex h-11 items-center gap-2.5 rounded-md pr-2 pl-1.5",
          "border border-ink-faint bg-surface-raised",
          "transition-colors duration-150 ease-out hover:bg-surface-sunk",
          focusRing,
        )}
      >
        <Avatar user={user} />
        <span className="hidden max-w-32 truncate font-sans text-body-sm font-medium text-ink lg:inline">
          {shortName(user)}
        </span>
        <Chevron open={open} />
        <span className="sr-only">Your account</span>
      </button>

      <div
        id={menuId}
        role="menu"
        aria-label="Your account"
        hidden={!open}
        className={cn(
          "absolute right-0 z-40 mt-2 w-64 overflow-hidden rounded-lg",
          "border border-rule bg-surface-raised shadow-lift",
        )}
      >
        {/*
          The address, shown once and not truncated away. Someone with two
          accounts — one from a password, one from Google — needs to be able to
          tell at a glance which one they are in, and this is the only place
          the site says so.
        */}
        <div className="border-b border-rule px-4 py-3">
          <p className="font-sans text-body-sm font-medium text-ink">{shortName(user)}</p>
          <p className="mt-0.5 font-sans text-caption break-all text-ink-soft">{user.email}</p>
        </div>

        <ul className="py-1">
          <li>
            <Link
              href="/account"
              role="menuitem"
              tabIndex={-1}
              onKeyDown={onItemKeyDown}
              className={cn(
                "flex min-h-11 items-center px-4",
                "font-sans text-body-sm text-ink",
                "transition-colors duration-150 ease-out hover:bg-accent-wash",
                focusRing,
              )}
            >
              Your account
            </Link>
          </li>
          {user.role === "ADMIN" ? (
            <li>
              {/*
                Phase 7 builds /admin/feedback. The entry point is here now
                because the role already exists and an admin with no way to
                reach their own queue is a link somebody adds badly later.
              */}
              <Link
                href="/admin/feedback"
                role="menuitem"
                tabIndex={-1}
                onKeyDown={onItemKeyDown}
                className={cn(
                  "flex min-h-11 items-center px-4",
                  "font-sans text-body-sm text-ink",
                  "transition-colors duration-150 ease-out hover:bg-accent-wash",
                  focusRing,
                )}
              >
                Moderation queue
              </Link>
            </li>
          ) : null}
          <li>
            <button
              type="button"
              role="menuitem"
              tabIndex={-1}
              disabled={signingOut}
              onKeyDown={onItemKeyDown}
              onClick={() => void onSignOut()}
              className={cn(
                "flex min-h-11 w-full items-center px-4 text-left",
                "font-sans text-body-sm text-ink",
                "transition-colors duration-150 ease-out hover:bg-accent-wash",
                "disabled:cursor-not-allowed disabled:text-(--color-disabled-text)",
                focusRing,
              )}
            >
              {signingOut ? "Signing out…" : "Sign out"}
            </button>
          </li>
        </ul>

        {/*
          Sign-out failed and the session is still live on the server.
          `role="alert"` because this appears after an action the person took
          and contradicts what they expect to have happened - it has to be
          announced, not just drawn. The wording says the state plainly rather
          than apologising: someone on a shared computer needs to know they are
          still signed in, not that we are sorry.
        */}
        {signOutError !== null ? (
          <div role="alert" className="border-t border-rule px-4 py-3">
            <p className="font-sans text-caption text-danger">
              You are still signed in — sign-out did not complete. {signOutError}
            </p>
          </div>
        ) : null}
      </div>
    </div>
  );
}

/**
 * One or two letters on a accent wash.
 *
 * `aria-hidden`, because the name it abbreviates is already in the trigger's
 * accessible name. Announcing "A" before it would be noise.
 */
function Avatar({ user }: { user: UserSummary }) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        "inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full",
        "bg-accent-wash font-sans text-body-sm font-semibold text-(--color-text-accent)",
      )}
    >
      {initials(user)}
    </span>
  );
}

function Chevron({ open }: { open: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      focusable="false"
      className={cn(
        "h-4 w-4 text-ink-soft transition-transform duration-150 ease-out",
        open && "rotate-180",
      )}
    >
      <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
    </svg>
  );
}

/**
 * The name to show, falling back to the local part of the address.
 *
 * `displayName` is optional at registration and Google's `name` claim is not
 * guaranteed either, so "there is always a name" is not something this can
 * assume.
 */
function shortName(user: UserSummary): string {
  const name = user.displayName?.trim();
  if (name) return name;
  const local = user.email.split("@")[0] ?? user.email;
  return local;
}

function initials(user: UserSummary): string {
  const source = shortName(user);
  const words = source.split(/[\s._-]+/).filter(Boolean);
  const first = words[0]?.[0] ?? source[0] ?? "?";
  const second = words.length > 1 ? (words[words.length - 1]?.[0] ?? "") : "";
  return (first + second).toUpperCase();
}
