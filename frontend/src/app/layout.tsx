import type { Metadata, Viewport } from "next";
import type { ReactNode } from "react";
import { Fraunces, Karla } from "next/font/google";
import { Navbar } from "@/components/layout/Navbar";
import { Footer } from "@/components/layout/Footer";
import "@/styles/globals.css";

/*
 * Both faces are self-hosted: next/font downloads them at build time and serves
 * them from our own origin, so there is no runtime request to Google and no
 * third party learns who visited. The cost is a network dependency during
 * `docker compose build`, which `npm ci` already imposes.
 *
 * The CSS variable names are deliberately the font names rather than
 * `--font-sans` / `--font-display`: the Tailwind theme keys in globals.css are
 * called that, and `--font-sans: var(--font-sans)` would be a self-reference
 * that silently drops the font.
 */
const fraunces = Fraunces({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-fraunces",
  weight: ["400", "600", "700"],
});

const karla = Karla({
  subsets: ["latin"],
  display: "swap",
  variable: "--font-karla",
  weight: ["400", "500", "600"],
});

export const metadata: Metadata = {
  title: {
    default: "HeadHeartFreeS",
    template: "%s · HeadHeartFreeS",
  },
  description: "A quiet place to put down what you are carrying.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  // No `themeColor`. It would tint mobile browser chrome to match the page,
  // but <meta name="theme-color"> is read independently of CSS and cannot
  // reference --hhf-bone, so setting it means a second hardcoded copy of the
  // background colour that drifts the first time the palette changes. Left
  // off deliberately; see PHASE_LOG.md Phase 2.
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    // data-scroll-behavior opts in explicitly to the smooth scrolling set on
    // <html> in globals.css. Without it Next warns at runtime that it cannot
    // tell deliberate smooth scrolling from an accidental inherited rule, and
    // may override it during router navigations.
    // https://nextjs.org/docs/messages/missing-data-scroll-behavior
    <html
      lang="en"
      data-scroll-behavior="smooth"
      className={`${fraunces.variable} ${karla.variable}`}
    >
      <body className="flex min-h-dvh flex-col">
        {/*
          Keyboard users land here first. It is visually hidden until focused,
          then sits over the header rather than shifting the layout.
        */}
        <a
          href="#main"
          className="sr-only focus-visible:not-sr-only focus-visible:absolute focus-visible:top-3 focus-visible:left-3 focus-visible:z-50 focus-visible:rounded-md focus-visible:border focus-visible:border-ink-faint focus-visible:bg-surface-raised focus-visible:px-4 focus-visible:py-3 focus-visible:text-body-sm focus-visible:text-ink"
        >
          Skip to content
        </a>

        <Navbar />

        <main id="main" className="app-layer flex-1">
          {children}
        </main>

        <Footer />
      </body>
    </html>
  );
}
