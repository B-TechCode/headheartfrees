import type { Metadata } from "next";
import { PageHeader } from "@/components/sections/PageHeader";
import { VoicesList } from "@/components/sections/VoicesList";

export const metadata: Metadata = {
  title: "Voices",
  description:
    "Notes people chose to leave after venting, published once a person has read them.",
};

/*
 * /voices
 *
 * The honest replacement for the original design's fabricated testimonials.
 * Phase 3 removed those and put `Promises` in their place on the home page,
 * with the note that nothing on this site would claim a visitor count, a
 * satisfaction figure or an attributed quote it did not have. This page is
 * where real ones go, and it stays empty until there are some.
 *
 * Public. It follows a flow that requires no account (rule 2.2), and reading
 * what other people chose to publish should not need one either.
 */
export default function VoicesPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
      <PageHeader
        eyebrow="Voices"
        title="What people have said"
        lede="Notes left by people who used this place, published only after someone has read them. Nothing here is invented, and nothing is quoted from a vent — those words were never sent to us."
      />

      <VoicesList />
    </div>
  );
}
