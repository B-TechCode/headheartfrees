import type { Metadata } from "next";
import { AdminFeedbackScreen } from "@/components/sections/AdminFeedbackScreen";

export const metadata: Metadata = {
  title: "Moderation queue",
  description: "Reviewing submitted notes.",
  // Never indexed. It is ADMIN-only and its content is other people's
  // unreviewed writing.
  robots: { index: false, follow: false },
};

/*
 * /admin/feedback
 *
 * Closes the 404 the avatar menu has been linking to since phase 6, where the
 * link was added ahead of the page on the reasoning that an admin with no
 * route to their own queue is a link somebody adds badly later.
 *
 * The role check that matters is on the server: `@PreAuthorize("hasRole('ADMIN')")`
 * on AdminFeedbackController, plus the path rule in SecurityConfig, with
 * AdminFeedbackAuthorisationIT pinning 401/403/200. What happens here is only
 * so that a USER who follows a bookmark sees a sentence rather than an empty
 * screen full of failed requests.
 */
export default function AdminFeedbackPage() {
  return <AdminFeedbackScreen />;
}
