import Link from "next/link";
import { API_BASE_URL } from "@/lib/api";

/*
 * Still a scaffold placeholder. The real home page — asymmetric hero and all —
 * is Phase 3; this exists so the layout, tokens and fonts have something to
 * render against, and so the design-system route is reachable without typing
 * the URL. It uses the primitives rather than inventing styles, so nothing here
 * has to be un-picked later.
 */
export default function HomePage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-16 sm:px-6 lg:py-24">
      <p className="font-sans text-overline font-semibold tracking-[0.085em] text-ink-soft uppercase">
        Phase 2
      </p>
      <h1 className="mt-3 font-display text-h1 text-ink">The design system is in place.</h1>
      <p className="mt-4 text-body-lg text-ink-soft">
        Tokens, type scale, primitives, navigation and the grain overlay are built. The home
        page itself lands in Phase 3.
      </p>

      <p className="mt-8 text-body-sm text-ink-soft">
        <Link
          href="/design-system"
          className="rounded-sm underline decoration-clay underline-offset-4 hover:text-(--color-text-accent)"
        >
          View the component preview
        </Link>
      </p>

      <p className="mt-10 text-caption text-ink-soft">
        API base URL: <code className="font-mono">{API_BASE_URL}</code>
      </p>
    </div>
  );
}
