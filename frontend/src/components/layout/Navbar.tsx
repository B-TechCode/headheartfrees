"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { cn } from "@/lib/cn";
import { Wordmark } from "@/components/ui/Logo";
import { focusRing } from "@/components/ui/styles";

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
  { href: "/about", label: "About" },
] as const;

export function Navbar() {
  const pathname = usePathname();
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
    <header className="app-layer border-b border-rule bg-surface">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-4 sm:px-6 lg:h-20 lg:px-8">
        <Link
          href="/"
          className={cn("rounded-sm text-ink transition-colors hover:text-clay-hover", focusRing)}
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
          <Link
            href="/login"
            className={cn(
              "hidden md:inline-flex md:items-center md:justify-center",
              "h-11 rounded-md border border-ink-faint bg-surface-raised px-4",
              "font-sans text-body-sm font-medium text-ink",
              "transition-[background-color,border-color] duration-150 ease-out",
              "hover:bg-surface-sunk",
              focusRing,
            )}
          >
            Sign in
          </Link>

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
                    "hover:bg-clay-wash",
                    isCurrent(link.href) && "font-medium",
                    focusRing,
                  )}
                >
                  {link.label}
                </Link>
              </li>
            ))}
            <li className="mt-2 border-t border-rule pt-2">
              <Link
                href="/login"
                className={cn(
                  "flex min-h-12 items-center rounded-md px-3",
                  "font-sans text-body font-medium text-ink",
                  "transition-colors duration-150 ease-out hover:bg-clay-wash",
                  focusRing,
                )}
              >
                Sign in
              </Link>
            </li>
          </ul>
        </nav>
      </div>
    </header>
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
        // The current page is marked by a short clay rule under the label, not
        // by a filled pill. One accent, used once.
        current &&
          "text-ink font-medium after:absolute after:inset-x-3 after:bottom-1.5 after:h-px after:bg-clay",
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
