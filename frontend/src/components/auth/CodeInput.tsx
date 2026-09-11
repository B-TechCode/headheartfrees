"use client";

import { Input } from "@/components/ui/Input";
import { cn } from "@/lib/cn";

/**
 * The six-digit field.
 *
 * ===========================================================================
 * One input, not six boxes
 * ===========================================================================
 *
 * The six-separate-boxes pattern is everywhere and it is worse in every way
 * that matters here. It needs a real `<label>` per box or none at all, so
 * screen readers announce six unlabelled fields; it fights the browser's
 * `one-time-code` autofill; it breaks paste unless every keystroke is
 * intercepted; and the focus-stealing between boxes is hostile to anybody
 * navigating by keyboard or using a screen magnifier.
 *
 * One labelled input does all of it for free: paste works because it is a text
 * field, autofill works because the browser recognises `one-time-code`, and
 * there is exactly one thing to announce and one thing to focus.
 *
 * ===========================================================================
 * Why the value is filtered rather than validated
 * ===========================================================================
 *
 * `sanitise` strips everything that is not a digit and truncates to six. So
 * pasting `123 456`, `123-456`, or a whole line copied out of a password
 * manager all land as `123456`, and there is no state in which the field holds
 * something that could not be submitted.
 *
 * Backup codes are the exception and take their own field — they are ten
 * characters of base32 with a dash, and running them through this filter would
 * silently eat them.
 */

export type CodeInputProps = {
  id: string;
  "aria-describedby": string | undefined;
  invalid: boolean;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  autoFocus?: boolean;
};

/** Digits only, at most six. */
export function sanitiseCode(raw: string): string {
  return raw.replace(/\D/g, "").slice(0, 6);
}

export function CodeInput({
  id,
  "aria-describedby": describedBy,
  invalid,
  value,
  onChange,
  disabled = false,
  autoFocus = false,
}: CodeInputProps) {
  return (
    <Input
      id={id}
      aria-describedby={describedBy}
      invalid={invalid}
      name="code"
      type="text"
      /*
       * `inputMode="numeric"` brings up the digit keypad on a phone without
       * type="number", which would add spinner arrows, accept `e` and `-`, and
       * let a scroll wheel over the focused field change the code.
       */
      inputMode="numeric"
      /*
       * The standard token. iOS and Android offer the code straight from the
       * SMS or authenticator notification, and password managers that hold
       * TOTP seeds fill it directly. It is the single highest-value attribute
       * on this field.
       */
      autoComplete="one-time-code"
      pattern="[0-9]*"
      /*
       * No `maxLength`, and that is deliberate rather than an omission.
       *
       * `maxLength` truncates the RAW input before any handler sees it. Paste
       * `123 456` - seven characters, which is how most authenticators and
       * password managers render a code - and the browser clips it to
       * `123 45`, which the filter below then turns into `12345`. Five digits,
       * a disabled button, and no indication of what went wrong.
       *
       * The cap lives in `sanitiseCode` instead, where it is applied after the
       * separators are removed. The field is controlled, so the value can
       * never exceed six either way. A test covers exactly this paste.
       */
      spellCheck={false}
      autoCapitalize="off"
      autoCorrect="off"
      value={value}
      // onChange rather than onPaste: a paste fires change too, so filtering
      // in one place covers typing, pasting, autofill and drag-and-drop
      // without four handlers that have to agree.
      onChange={(event) => onChange(sanitiseCode(event.target.value))}
      disabled={disabled}
      autoFocus={autoFocus}
      placeholder="000000"
      className={cn(
        // Wide tracking and a tabular figure set, so six digits are read as
        // six digits rather than as a number. Not a monospace stack: the sans
        // face already has tabular figures and switching family here would
        // make this the only input on the site in a different typeface.
        "font-sans text-h4 tracking-[0.35em] tabular-nums",
        // Sized for six characters plus the tracking, rather than stretching
        // to the form width — a full-width box for six digits invites the
        // wrong kind of input.
        "max-w-[11rem]",
      )}
    />
  );
}
