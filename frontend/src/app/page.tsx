import { API_BASE_URL } from "@/lib/api";

/*
 * Scaffold placeholder. The real home page is phase 3, on top of the design
 * system built in phase 2 - so this stays intentionally unstyled rather than
 * introducing visual decisions that would only be thrown away.
 */
export default function HomePage() {
  return (
    <main className="mx-auto flex min-h-dvh max-w-xl flex-col justify-center gap-4 px-6">
      <h1 className="text-2xl">HeadHeartFreeS</h1>
      <p className="text-ink-muted">
        Scaffold is up. Design system lands in phase 2, this page in phase 3.
      </p>
      <p className="text-ink-muted text-sm">
        API base URL: <code>{API_BASE_URL}</code>
      </p>
    </main>
  );
}
