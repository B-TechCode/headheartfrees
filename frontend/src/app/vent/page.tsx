import type { Metadata } from "next";
import { VentComposer } from "@/components/sections/VentComposer";

export const metadata: Metadata = {
  title: "Vent",
  description:
    "Write down what is weighing on you and let it go. Your words never leave your browser. No account needed.",
};

/*
 * /vent — the product.
 *
 * A server component wrapping one client island. The page shell, heading and
 * standfirst are static; only the composer needs state, so only the composer
 * ships JavaScript.
 *
 * Deliberately narrow (`max-w-2xl`) and deliberately not full-height. There is
 * no `dvh` or `vh` on this page at all: the composer sits in ordinary document
 * flow so that when a mobile keyboard opens, the page simply scrolls. See the
 * comment in VentComposer for why the obvious full-height layout is wrong here.
 *
 * Rule 2.2: no auth, no gate, no prompt. Someone arriving here can type
 * immediately.
 */
export default function VentPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-12 sm:px-6 lg:py-16">
      <header className="max-w-xl">
        <span aria-hidden="true" className="block h-px w-12 bg-accent" />
        <h1 className="mt-6 font-display text-h1 text-ink">Put it down here.</h1>
        <p className="mt-4 text-body-lg text-ink-soft">
          No one is reading this. Not us, not a machine. Write it, release it, and it is
          gone.
        </p>
      </header>

      <div className="mt-10">
        <VentComposer />
      </div>
    </div>
  );
}
