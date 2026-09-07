import type { Metadata } from "next";
import { Suspense } from "react";
import { PageHeader } from "@/components/sections/PageHeader";
import { LoginForm } from "@/components/auth/LoginForm";

export const metadata: Metadata = {
  title: "Sign in",
  description:
    "Sign in to HeadHeartFreeS. An account is only needed for feedback you choose to leave; venting never requires one.",
  // No account page should be indexed, and a sign-in form is not a search
  // result anyone wants. `follow` so the links off it are still crawled.
  robots: { index: false, follow: true },
};

/*
 * /login.
 *
 * The lede is doing real work. Someone arriving here from the navbar may not
 * know whether they need an account at all, and rule 2.2 says they do not —
 * so the page says so before it asks for anything.
 *
 * `Suspense` is required, not decorative: `LoginForm` calls `useSearchParams`,
 * and without a boundary Next opts the entire route out of static rendering
 * and says so at build time.
 */
export default function LoginPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        eyebrow="Account"
        title="Sign in"
        lede="You do not need an account to write here. This is only for the feedback you choose to leave, and for keeping it yours."
      />

      <Suspense fallback={<FormPlaceholder />}>
        <LoginForm />
      </Suspense>
    </div>
  );
}

/**
 * Reserved space, not a skeleton.
 *
 * A pulsing grey imitation of a form is the kind of default-toolkit gesture
 * §8 rules out, and this boundary resolves on the first client render anyway.
 * Holding the height stops the footer jumping.
 */
function FormPlaceholder() {
  return <div className="mt-10 h-96 max-w-md" aria-hidden="true" />;
}
