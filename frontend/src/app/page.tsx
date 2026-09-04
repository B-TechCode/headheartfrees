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
 * One consequence worth knowing: that placeholder held the only link to
 * /design-system. The preview route still exists and still works, but it is now
 * reachable only by typing the URL. That is the right end state for an internal
 * tool Phase 9 deletes, but it does mean it will not be stumbled upon.
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
