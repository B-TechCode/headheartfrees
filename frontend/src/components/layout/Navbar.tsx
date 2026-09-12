"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/cn";
import { Wordmark } from "@/components/ui/Logo";
import { focusRing } from "@/components/ui/styles";
import { AccountMenu } from "@/components/auth/AccountMenu";
import { useSession } from "@/lib/auth/SessionProvider";
import { signInHref } from "@/lib/auth/sign-in-href";
import { describeAuthError } from "@/lib/auth/errors";
import type { UserSummary } from "@/lib/auth/types";

/**
 * Primary navigation.
 *
 * PROJECT_BRIEF.md §7: `Home · Vent · About`, with Sign in on the right.
 * **Donate and Feedback must not appear here.** The previous design had them
 * and they were removed deliberately — asking someone for money or a review on
 * the way in to write about a hard day is the wrong order of operations. If a
 * future task adds either to this array, that task is wrong.
 */
const NAV_LINKS = [
  { href: "/", label: "Home" },
  { href: "/vent", label: "Vent" },
  // Added in phase 7, when there was something behind it. PROJECT_BRIEF.md §7
  // listed three items; the amendment is recorded there with a date, the same
  // way the /vent/stats correction and the footer's Navigate column were.
  //
  // The prohibition above is unchanged and is not about this: Donate and
  // Feedback must never appear here. /voices is neither - it is a page of what
  // other people wrote, not a request aimed at the reader.
  { href: "/voices", label: "Voices" },
  { href: "/about", label: "About" },
] as const;

export function Navbar() {
  const pathname = usePathname();
  const { status, user } = useSession();
  const [menuOpen, setMenuOpen] = useState(false);
  const toggleRef = useRef<HTMLButtonElement>(null);

  // Route change closes the menu. Without this the panel stays open over the
  // page the user just navigated to.
  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  // Escape closes and returns focus to the control that opened it.
  useEffect(() => {
    if (!menuOpen) return;

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setMenuOpen(false);
        toggleRef.current?.focus();
      }
    }

    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [menuOpen]);

  const isCurrent = (href: string) =>
    href === "/" ? pathname === "/" : pathname.startsWith(href);

  return (
    // `z-20`, not the `z-1` that `.app-layer` alone would give it.
    //
    // `.app-layer` sets `position: relative; z-index: 1` on the header, on
    // <main> and on the footer, to lift them off the paper-grain layer. That
    // makes each one its own stacking context, and three siblings at the same
    // z-index are painted in DOM order - so <main> lands on top of the header.
    //
    // The avatar dropdown is `absolute z-40` inside this header, and it hangs
    // below the 64px header box into <main>'s area. `z-40` only orders it
    // within the header's stacking context; it cannot lift it above a sibling
    // of the header. <main> has no background of its own (the surface colour
    // is on `body`), so the panel stayed visible while <main> took every click
    // that landed on it - the menu shut via the outside-pointerdown handler and
    // no handler ever ran. Sign out fired no request at all.
    //
    // Raising the header above <main> is what makes the panel's own z-index
    // mean something. Anything > 1 works; 20 leaves room under the skip link's
    // z-50 and reads as deliberate.
    <header className="app-layer z-20 border-b border-rule bg-surface">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 sm:px-6 lg:h-20 lg:px-8">
        <Link
          href="/"
          className={cn("rounded-sm text-ink transition-colors hover:text-accent-hover", focusRing)}
        >
          <Wordmark />
        </Link>

        {/*
          Left of centre, not centred: §8 rules out perfect symmetry, and a nav
          that sits just left of the midpoint reads as considered rather than
          as a default flex layout.
        */}
        <nav aria-label="Main" className="hidden md:flex md:items-center md:gap-1 lg:ml-4">
          {NAV_LINKS.map((link) => (
            <NavLink key={link.href} href={link.href} current={isCurrent(link.href)}>
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="ml-auto flex items-center gap-2">
          {/*
            ===================================================================
            The one rule for this corner
            ===================================================================

            While `status` is "restoring" this renders a reserved space and
            nothing else. It must never render "Sign in" and then swap it for
            an avatar: to someone who is signed in, a "Sign in" button
            appearing on load does not read as a loading state, it reads as
            having been logged out - on a site where being logged out could
            plausibly mean something happened to their account. A blank gap for
            one frame says nothing at all, which is the honest thing to say
            while we do not yet know.

            The width is reserved rather than left to collapse so the wordmark
            and the nav do not slide sideways when the answer arrives.
          */}
          <div className="hidden md:block">
            {status === "restoring" ? (
              <div className="h-11 w-28" aria-hidden="true" />
            ) : status === "authenticated" && user !== null ? (
              <AccountMenu user={user} />
            ) : (
              <Link
                href={signInHref(pathname)}
                className={cn(
                  "inline-flex h-11 w-28 items-center justify-center",
                  "rounded-md border border-ink-faint bg-surface-raised px-4",
                  "font-sans text-body-sm font-medium text-ink",
                  "transition-[background-color,border-color] duration-150 ease-out",
                  "hover:bg-surface-sunk",
                  focusRing,
                )}
              >
                Sign in
              </Link>
            )}
          </div>

          <button
            ref={toggleRef}
            type="button"
            aria-expanded={menuOpen}
            aria-controls="mobile-menu"
            aria-label={menuOpen ? "Close menu" : "Open menu"}
            onClick={() => setMenuOpen((open) => !open)}
            className={cn(
              "inline-flex h-11 w-11 items-center justify-center rounded-md md:hidden",
              "border border-ink-faint bg-surface-raised text-ink",
              "transition-colors duration-150 ease-out hover:bg-surface-sunk",
              focusRing,
            )}
          >
            <MenuIcon open={menuOpen} />
          </button>
        </div>
      </div>

      {/*
        Disclosure rather than a full-screen overlay. Covering the page to show
        three links is theatre, and it puts a modal between someone and /vent.
      */}
      <div
        id="mobile-menu"
        hidden={!menuOpen}
        className="border-t border-rule bg-surface-raised shadow-lift md:hidden"
      >
        <nav aria-label="Main" className="mx-auto max-w-6xl px-4 py-3 sm:px-6">
          <ul className="flex flex-col">
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <Link
                  href={link.href}
                  aria-current={isCurrent(link.href) ? "page" : undefined}
                  className={cn(
                    "flex min-h-12 items-center rounded-md px-3",
                    "font-sans text-body text-ink transition-colors duration-150 ease-out",
                    "hover:bg-accent-wash",
                    isCurrent(link.href) && "font-medium",
                    focusRing,
                  )}
                >
                  {link.label}
                </Link>
              </li>
            ))}
            {/*
              Same rule as the desktop corner: nothing session-shaped is
              rendered until the session is known. Here the panel simply has
              one fewer item for a frame, and everything above it stays put.
            */}
            {status === "restoring" ? null : status === "authenticated" && user !== null ? (
              <MobileAccountItems user={user} />
            ) : (
              <li className="mt-2 border-t border-rule pt-2">
                <Link
                  href={signInHref(pathname)}
                  className={cn(
                    "flex min-h-12 items-center rounded-md px-3",
                    "font-sans text-body font-medium text-ink",
                    "transition-colors duration-150 ease-out hover:bg-accent-wash",
                    focusRing,
                  )}
                >
                  Sign in
                </Link>
              </li>
            )}
          </ul>
        </nav>
      </div>
    </header>
  );
}

/**
 * The mobile equivalent of the avatar menu.
 *
 * Flat list items rather than a nested dropdown: the panel is already a
 * disclosure, and a menu inside a menu on a phone is two things to dismiss
 * where one will do.
 */
function MobileAccountItems({ user }: { user: UserSummary }) {
  const { signOut } = useSession();
  const router = useRouter();
  const [signOutError, setSignOutError] = useState<string | null>(null);

  const itemClasses = cn(
    "flex min-h-12 w-full items-center rounded-md px-3 text-left",
    "font-sans text-body text-ink",
    "transition-colors duration-150 ease-out hover:bg-accent-wash",
    focusRing,
  );

  return (
    <>
      <li className="mt-2 border-t border-rule pt-3">
        <p className="px-3 font-sans text-caption break-all text-ink-soft">{user.email}</p>
      </li>
      <li className="mt-1">
        <Link href="/account" className={itemClasses}>
          Your account
        </Link>
      </li>
      {user.role === "ADMIN" ? (
        <li>
          <Link href="/admin/feedback" className={itemClasses}>
            Moderation queue
          </Link>
        </li>
      ) : null}
      <li>
        <button
          type="button"
          onClick={() => {
            setSignOutError(null);
            // A failed sign-out must not navigate: the session is still live
            // on the server, and "/" with a Sign in button would say otherwise.
            void signOut().then(
              () => router.push("/"),
              (error: unknown) => setSignOutError(describeAuthError(error)),
            );
          }}
          className={itemClasses}
        >
          Sign out
        </button>
      </li>
      {signOutError !== null ? (
        <li role="alert" className="px-3 pt-1 pb-2">
          <p className="font-sans text-caption text-danger">
            You are still signed in — sign-out did not complete. {signOutError}
          </p>
        </li>
      ) : null}
    </>
  );
}

function NavLink({
  href,
  current,
  children,
}: {
  href: string;
  current: boolean;
  children: React.ReactNode;
}) {
  return (
    <Link
      href={href}
      aria-current={current ? "page" : undefined}
      className={cn(
        "relative inline-flex h-11 items-center rounded-sm px-3",
        "font-sans text-body-sm text-ink-soft",
        "transition-colors duration-150 ease-out hover:text-ink",
        // The current page is marked by a short accent rule under the label, not
        // by a filled pill. One accent, used once.
        current &&
          "text-ink font-medium after:absolute after:inset-x-3 after:bottom-1.5 after:h-px after:bg-accent",
        focusRing,
      )}
    >
      {children}
    </Link>
  );
}

/** Hand-drawn line icon: three rules that collapse to a cross. No icon library. */
function MenuIcon({ open }: { open: boolean }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" focusable="false" className="h-5 w-5">
      {open ? (
        <>
          <path d="M6 6 18 18" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
          <path d="M18 6 6 18" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
        </>
      ) : (
        <>
          <path d="M4 7h16" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
          <path d="M4 12h16" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
          <path d="M4 17h10" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
        </>
      )}
    </svg>
  );
}
