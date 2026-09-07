import type { Metadata } from "next";
import { Suspense } from "react";
import { PageHeader } from "@/components/sections/PageHeader";
import { RegisterForm } from "@/components/auth/RegisterForm";

export const metadata: Metadata = {
  title: "Create an account",
  description:
    "Create a HeadHeartFreeS account. Optional: venting works without one and always will.",
  robots: { index: false, follow: true },
};

/*
 * /register.
 *
 * The lede states what an account is for and, more importantly, what it is not
 * for. Every other sign-up page on the internet is trying to convert; this one
 * is trying not to let anyone believe an account is the price of using the
 * site, because it is not, and someone who believes it might leave instead.
 */
export default function RegisterPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        eyebrow="Account"
        title="Create an account"
        lede="Optional, and it changes nothing about venting. An account exists so the feedback you leave stays attached to you, and so you can take it down again."
      />

      <Suspense fallback={<FormPlaceholder />}>
        <RegisterForm />
      </Suspense>
    </div>
  );
}

function FormPlaceholder() {
  return <div className="mt-10 h-[32rem] max-w-md" aria-hidden="true" />;
}
