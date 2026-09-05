"use client";

import { useRouter } from "next/navigation";
import { useCallback, useId, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { Textarea } from "@/components/ui/Textarea";
import { MoodIcon } from "@/components/ui/MoodIcon";
import { CrisisPanel } from "@/components/sections/CrisisPanel";
import { cn } from "@/lib/cn";
import { focusRing } from "@/components/ui/styles";
import { apiFetch } from "@/lib/api";
import { MOODS, type Mood } from "@/lib/moods";
import { containsCrisisLanguage } from "@/lib/safety";

const MAX_LENGTH = 2000;

/**
 * Optional openings for people who have something to say and no idea where to
 * start. Inserted as a line to write under, and deletable like anything else —
 * they are a door, not a form.
 */
const STARTERS = [
  "What I actually wanted to say was",
  "The part nobody knows is",
  "I keep going over",
  "What I am tired of is",
] as const;

/**
 * The vent composer.
 *
 * ===========================================================================
 * PROJECT_BRIEF.md rule 2.1 — read before changing anything here
 * ===========================================================================
 *
 * `text` below is the single most sensitive value in this codebase. It lives in
 * React state for the life of this component and nowhere else:
 *
 *   - It is NEVER put in `localStorage` or `sessionStorage`. There is no draft
 *     recovery and there will not be one; closing the tab is supposed to lose
 *     it.
 *   - It is NEVER put in a cookie, a URL, a query string, a route param or a
 *     hash.
 *   - It is NEVER passed to `apiFetch`. The release call sends `{ mood }`, and
 *     `mood` is one of six constants.
 *   - It is NEVER logged, and never included in an error report — see the
 *     catch in `release()`, which deliberately ignores the error object.
 *   - It does not survive navigation. Pushing to /vent/released unmounts this
 *     component and the string becomes garbage.
 *
 * The crisis check reads it, in this browser, and returns a boolean. That is
 * the only thing that ever inspects it.
 *
 * ===========================================================================
 * Mobile keyboard
 * ===========================================================================
 *
 * No fixed heights and no `vh` anywhere in this component. The composer is
 * ordinary document flow, so when the on-screen keyboard opens and the viewport
 * shrinks, the page scrolls normally and the button stays reachable. A
 * full-height flex column with the button pinned to the bottom — the obvious
 * layout for this screen — is precisely what puts the button under the keyboard
 * on a short viewport.
 */
export function VentComposer() {
  const router = useRouter();
  const textareaId = useId();
  const counterId = useId();
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const [text, setText] = useState("");
  const [mood, setMood] = useState<Mood | null>(null);
  const [releasing, setReleasing] = useState(false);

  /**
   * Sticky once true.
   *
   * Recomputing on every keystroke would make the panel flicker in and out as
   * someone writes and rewrites a sentence, which reads as the page reacting to
   * individual words and is unpleasant when the words are these ones. Once
   * shown it stays for the rest of the session.
   */
  const [showCrisisPanel, setShowCrisisPanel] = useState(false);

  const onTextChange = useCallback(
    (value: string) => {
      setText(value);
      if (!showCrisisPanel && containsCrisisLanguage(value)) {
        setShowCrisisPanel(true);
      }
    },
    [showCrisisPanel],
  );

  const insertStarter = useCallback(
    (starter: string) => {
      setText((current) => {
        const prefix = current.trim().length > 0 ? `${current.replace(/\s+$/, "")}\n\n` : "";
        return `${prefix}${starter} `;
      });
      // Put the caret at the end so they can simply carry on typing.
      requestAnimationFrame(() => {
        const el = textareaRef.current;
        if (el) {
          el.focus();
          el.setSelectionRange(el.value.length, el.value.length);
        }
      });
    },
    [],
  );

  const release = useCallback(async () => {
    setReleasing(true);

    // The body is `{ mood }`. There is no branch of this function in which the
    // textarea's contents are included.
    try {
      await apiFetch<void>("/api/v1/vent/release", {
        method: "POST",
        body: { mood },
      });
    } catch {
      // Deliberately swallowed, and deliberately not bound to a variable.
      //
      // The count is incidental — it is a counter, not the product. Someone who
      // pressed release must never be shown an error about words they were
      // promised were private, because the only reasonable reading of such an
      // error is "something went wrong with your message", which is exactly the
      // fear this site exists to remove. So: clear and continue regardless.
      //
      // Nothing is reported anywhere, because a report would be the one place
      // the text could plausibly leak into.
    }

    setText("");
    setMood(null);
    router.push("/vent/released");
  }, [mood, router]);

  const remaining = MAX_LENGTH - text.length;
  const canRelease = text.trim().length > 0 && !releasing;

  return (
    <div className="flex flex-col gap-8">
      {/* ---- Mood chips ------------------------------------------------- */}
      <fieldset className="border-0 p-0">
        <legend className="font-sans text-body-sm font-medium text-ink">
          How does it feel right now?{" "}
          <span className="font-normal text-ink-soft">optional</span>
        </legend>

        <div className="mt-3 flex flex-wrap gap-2">
          {MOODS.map((option) => (
            <Chip
              key={option.value}
              selected={mood === option.value}
              icon={<MoodIcon mood={option.value} />}
              onClick={() => setMood(mood === option.value ? null : option.value)}
            >
              {option.label}
            </Chip>
          ))}
        </div>
      </fieldset>

      {/* ---- Prompt starters -------------------------------------------- */}
      <div>
        <p className="font-sans text-body-sm font-medium text-ink">
          Or start somewhere <span className="font-normal text-ink-soft">optional</span>
        </p>
        <div className="mt-3 flex flex-wrap gap-2">
          {STARTERS.map((starter) => (
            <button
              key={starter}
              type="button"
              onClick={() => insertStarter(starter)}
              className={cn(
                "inline-flex min-h-11 items-center rounded-md px-3.5 py-2",
                "border border-rule bg-surface text-left",
                "font-sans text-body-sm text-ink-soft",
                "transition-colors duration-150 ease-out",
                "hover:border-ink-faint hover:text-ink",
                focusRing,
              )}
            >
              {starter}&hellip;
            </button>
          ))}
        </div>
      </div>

      {/* ---- The box ----------------------------------------------------- */}
      <div>
        <label htmlFor={textareaId} className="font-sans text-body-sm font-medium text-ink">
          What is weighing on you?
        </label>

        <Textarea
          id={textareaId}
          ref={textareaRef}
          value={text}
          onChange={(event) => onTextChange(event.target.value)}
          maxLength={MAX_LENGTH}
          rows={10}
          aria-describedby={counterId}
          placeholder="Start anywhere. It does not have to make sense."
          className="mt-3 min-h-56"
        />

        <div className="mt-2 flex items-baseline justify-between gap-4">
          <p
            id={counterId}
            aria-live="polite"
            className={cn(
              "text-caption tabular-nums",
              remaining <= 100 ? "text-(--color-text-accent)" : "text-ink-soft",
            )}
          >
            {text.length} / {MAX_LENGTH}
          </p>
        </div>
      </div>

      {/* ---- Crisis panel, above the button per §7 ----------------------- */}
      {showCrisisPanel ? <CrisisPanel /> : null}

      {/* ---- Release ----------------------------------------------------- */}
      <div>
        <Button
          size="lg"
          fullWidth
          disabled={!canRelease}
          loading={releasing}
          loadingLabel="Releasing"
          onClick={release}
          className="sm:w-auto"
        >
          Release &amp; Let Go
        </Button>

        <p className="mt-4 max-w-[60ch] text-body-sm text-ink-soft">
          Nothing you have written is sent anywhere. Pressing the button clears it from this
          page, and there is no copy of it to delete.
        </p>
      </div>
    </div>
  );
}
