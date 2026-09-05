/**
 * ============================================================================
 * THIS IS A SUPPLEMENT, NOT A SAFETY NET.
 * ============================================================================
 *
 * This list will miss things. It will miss people writing around the words,
 * writing in a language it does not cover, writing in metaphor, or writing
 * nothing at all about how bad it has got. Do not treat a non-match as evidence
 * that someone is fine, and do not let its existence justify making the
 * helplines harder to find anywhere else.
 *
 * The real coverage is structural: the footer helpline strip renders on every
 * page of this site, on every route, including 404s. It is there whether this
 * file matches or not. What this adds is a nudge closer to the moment — the
 * numbers appearing beside the box someone is typing into, rather than at the
 * bottom of the page.
 *
 * ---------------------------------------------------------------------------
 * Nothing here is transmitted
 * ---------------------------------------------------------------------------
 *
 * The check runs in the browser against text that never leaves it. No match is
 * reported, counted, logged or sent. There is no endpoint that would accept it.
 * The result exists only to decide whether a panel renders. PROJECT_BRIEF.md
 * rule 2.1.
 *
 * ---------------------------------------------------------------------------
 * Why the list is this narrow
 * ---------------------------------------------------------------------------
 *
 * Precision matters more than recall here, which is the opposite of most
 * matching problems. A panel that appears while someone writes about an
 * ordinary bad week teaches them the site over-reacts, and the next time it
 * appears — when it matters — they have already learned to skim past it. Every
 * false positive spends the credibility of the true one.
 *
 * So the list matches **first-person statements of intent**, never distress
 * vocabulary. The distinction doing most of the work: `kill myself` is in,
 * `kill me` is out, because "kill me now" is something people say about a
 * meeting.
 *
 * Deliberately NOT matched, each of which is ordinary vent language:
 *
 *   depressed · hopeless · worthless · empty · alone · nobody cares
 *   hate myself · can't go on · give up · disappear · sleep forever
 *   die / dead / kill on their own
 *
 * `numb` is not matched either, and could not be: it is one of our own mood
 * chips.
 *
 * ---------------------------------------------------------------------------
 * Hindi and Hinglish — NEEDS A NATIVE SPEAKER'S REVIEW BEFORE LAUNCH
 * ---------------------------------------------------------------------------
 *
 * The English set is at a precision I can reason about. The Hinglish set below
 * is deliberately tiny: only unambiguous nouns and explicit first-person
 * intent. Transliteration varies between writers (mar jaunga / mar jaoonga /
 * mar jaunga), and hyperbolic references to dying are *more* common in casual
 * Hinglish than in English — "itna kaam hai mar jaunga" means "there is so much
 * work I'll die". That hyperbole is exactly what would generate the false
 * positives this list is designed to avoid, and I cannot judge its frequency
 * the way I can in English.
 *
 * A partial high-precision list serves Hindi speakers better than an
 * English-only one, so it ships. It must be reviewed by a Hindi speaker before
 * this site is public. Flagged in PROJECT_BRIEF.md §7 as well as here.
 *
 * ---------------------------------------------------------------------------
 * If you are editing this list
 * ---------------------------------------------------------------------------
 *
 * Adding a term is not free. Ask whether a person having a bad but ordinary day
 * could plausibly write it. If yes, leave it out.
 */

/**
 * Phrases indicating intent to end one's life or to cause self-harm.
 *
 * Some entries name a method. They are detection triggers only — the panel's
 * copy never repeats them and never names a method. See `CrisisPanel`.
 */
const INTENT_PATTERNS: readonly string[] = [
  // Ending one's life — explicit, first person.
  "kill myself",
  "killing myself",
  "end my life",
  "ending my life",
  "take my own life",
  "taking my own life",
  "end it all",
  "want to die",
  "wanna die",
  "better off dead",
  "no reason to live",
  "no point in living",
  "dont want to be alive",
  "do not want to be alive",
  "dont want to live",
  "do not want to live",
  "dont want to be here",
  "do not want to be here",
  "never wake up",
  "not want to wake up",
  "dont want to wake up",

  // Named directly.
  "suicidal",
  "suicide",
  "commit suicide",

  // Self-harm, first person.
  "hurt myself",
  "harm myself",
  "cut myself",
  "cutting myself",
  "self harm",

  // Method references. Trigger only; never echoed back.
  "hang myself",
  "overdose",
  "jump off",

  // Hindi / Hinglish. Narrow by design. NEEDS REVIEW — see header.
  "khudkushi",
  "atmahatya",
  "jeena nahi chahta",
  "jeena nahi chahti",
  "marna chahta hun",
  "marna chahti hun",
];

/**
 * Lowercases, drops apostrophes so `don't` and `dont` are one case, turns
 * hyphens and underscores into spaces so `self-harm` matches `self harm`, and
 * collapses runs of whitespace.
 *
 * Punctuation is otherwise left alone: the patterns are matched on word
 * boundaries, so a trailing full stop or comma does not prevent a match.
 */
function normalise(input: string): string {
  return input
    .toLowerCase()
    .replace(/['‘’]/g, "")
    .replace(/[-_]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/** Escapes a literal phrase for use inside a RegExp. */
function escapeForRegExp(literal: string): string {
  return literal.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * Compiled once. Word-boundary anchored so `overdose` does not fire inside a
 * longer word, and so a phrase still matches when it ends a sentence.
 */
const COMPILED: readonly RegExp[] = INTENT_PATTERNS.map(
  (pattern) => new RegExp(`\\b${escapeForRegExp(pattern)}\\b`, "i"),
);

/**
 * True when the text contains a phrase indicating intent.
 *
 * Returns a boolean and nothing else — deliberately not which phrase matched.
 * The caller has no legitimate use for that, and a function that returned it
 * would be one refactor away from something logging it.
 *
 * @param text the textarea contents. Never leaves the browser.
 */
export function containsCrisisLanguage(text: string): boolean {
  if (!text) {
    return false;
  }
  const haystack = normalise(text);
  return COMPILED.some((pattern) => pattern.test(haystack));
}
