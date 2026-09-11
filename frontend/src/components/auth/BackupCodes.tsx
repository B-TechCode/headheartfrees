"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Callout } from "@/components/ui/Callout";
import { cn } from "@/lib/cn";

/**
 * The recovery codes, shown once.
 *
 * ===========================================================================
 * "Once" is literal, and the screen has to say so
 * ===========================================================================
 *
 * The codes are Argon2id hashes on the server the moment they are generated,
 * and there is no endpoint that returns them again. Close this without copying
 * them and they are gone: the only way back to a full set is to generate new
 * ones with a working authenticator, and the only way past a lost
 * authenticator with no codes is the developer's SQL.
 *
 * So the warning is a `caution` panel above the codes rather than a line of
 * small print under them, and the acknowledgement is an explicit button. That
 * button gates nothing on the server — the codes are already committed — but
 * it is the difference between somebody reading the sentence and somebody
 * scrolling past it.
 *
 * ===========================================================================
 * Copy, and why there is no download
 * ===========================================================================
 *
 * `navigator.clipboard` is not available in every context — an insecure
 * origin, or a browser that refuses without a user gesture it recognises — so
 * the copy button reports failure rather than pretending, and the codes are
 * selectable text underneath regardless. A file download was considered and
 * left out: it writes the account's recovery material to the Downloads folder,
 * where it stays, unencrypted, long after the person has forgotten it is
 * there.
 */
export function BackupCodes({
  codes,
  onAcknowledge,
  acknowledgeLabel = "I have saved these",
}: {
  codes: string[];
  onAcknowledge: () => void;
  acknowledgeLabel?: string;
}) {
  const [copied, setCopied] = useState<"idle" | "done" | "failed">("idle");

  async function copy() {
    try {
      await navigator.clipboard.writeText(codes.join("\n"));
      setCopied("done");
    } catch {
      // Not an error worth a panel. The codes are on screen and selectable,
      // so the person has a way through either way - they just have to do it
      // by hand.
      setCopied("failed");
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <Callout tone="caution" title="Save these now — you will not see them again">
        <p>
          Each code signs you in once if you lose your phone. Keep them somewhere you can reach
          without this site: a password manager, or paper.
        </p>
      </Callout>

      <ul
        className={cn(
          "grid grid-cols-1 gap-x-8 gap-y-2 rounded-lg border border-rule bg-surface-sunk p-5",
          "sm:grid-cols-2",
        )}
      >
        {codes.map((code) => (
          <li
            key={code}
            className="font-sans text-body tracking-[0.14em] tabular-nums text-ink select-all"
          >
            {code}
          </li>
        ))}
      </ul>

      <div className="flex flex-wrap items-center gap-4">
        <Button type="button" variant="secondary" onClick={copy}>
          Copy all
        </Button>
        <Button type="button" onClick={onAcknowledge}>
          {acknowledgeLabel}
        </Button>

        <p role="status" aria-live="polite" className="text-body-sm text-ink-soft">
          {copied === "done"
            ? "Copied."
            : copied === "failed"
              ? "Could not copy automatically — select them and copy by hand."
              : ""}
        </p>
      </div>
    </div>
  );
}
