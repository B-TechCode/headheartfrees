import type { Metadata } from "next";
import { Hero } from "@/components/sections/Hero";
import { HowItWorks } from "@/components/sections/HowItWorks";
import { Promises } from "@/components/sections/Promises";
import { ClosingAction } from "@/components/sections/ClosingAction";

export const metadata: Metadata = {
  // The root layout appends " · HeadHeartFreeS" to every title. Home sets an
  // absolute one so it does not read "HeadHeartFreeS · HeadHeartFreeS".
  title: {
    absolute: "HeadHeartFreeS — somewhere to put it down",
  },
  description:
    "Write down what is weighing on you, then let it go. Your words never leave your browser. No account, no advice, no reply. Free.",
};

/*
 * Home.
 *
 * The Phase 2 placeholder that stood here is gone in full, including the
 * "API base URL" debug line. Nothing from it survives.
 *
 * That placeholder held the only link to /design-system, the internal preview
 * of every primitive. Phase 9 deleted that route, as PROJECT_BRIEF.md §7 said
 * it would: it existed to build the design system against, it shipped no
 * product surface, and an internal tool left on a public origin is a page
 * nobody maintains and everybody can read.
 */
export default function HomePage() {
  return (
    <>
      <Hero />
      <HowItWorks />
      <Promises />
      <ClosingAction />
    </>
  );
}
