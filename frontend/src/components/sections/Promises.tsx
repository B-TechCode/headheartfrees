/**
 * What this promises.
 *
 * This section replaces the testimonials and the counter block from the
 * original design. That material was fabricated — invented visit counts,
 * invented countries, invented named reviewers — and none of it is reproduced
 * here in any form, including as placeholder or commented-out markup.
 *
 * Every line below is a property of the product as built, checkable against the
 * code today. Nothing is aspirational and nothing is a number. When the vent
 * counter goes live in Phase 6 there will be one real figure to add; until then
 * the page argues from what the thing actually does, which is unusual enough to
 * carry it.
 */
const PROMISES = [
  {
    title: "No account, ever",
    body: "You do not sign up to write. There is no email box between you and the page. Accounts exist later for people who want to leave feedback under a name, and never for venting.",
  },
  {
    title: "Your words are never sent",
    body: "Not stored, not logged, not held briefly and deleted. The text stays in your browser and is cleared when you release it. There is no endpoint on our side that accepts it.",
  },
  {
    title: "No advice, no reply",
    body: "Nothing analyses what you wrote. No AI responds, no counsellor is assigned, no follow-up email arrives. Some things do not need answering.",
  },
  {
    title: "English, Hindi, or Hinglish",
    body: "Write however you actually think. Nothing is parsed, so nothing needs to be in a language a machine can read.",
  },
  {
    title: "Free",
    body: "No trial, no tier, no upgrade. If you want to help keep it running you can, and it changes nothing about what you get.",
  },
] as const;

export function Promises() {
  return (
    <section aria-labelledby="promises-heading" className="border-b border-rule bg-surface-raised">
      <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:px-8 lg:py-24">
        <div className="max-w-2xl">
          <span aria-hidden="true" className="block h-px w-12 bg-clay" />
          <h2 id="promises-heading" className="mt-6 font-display text-h2 text-ink">
            What this promises
          </h2>
          <p className="mt-4 text-body-lg text-ink-soft">
            Five things, all of which you can check against how the site behaves.
          </p>
        </div>

        {/*
          Two columns from md, with the list running down the first then the
          second. Not a grid of equal cards — §8 rules out repeating the same
          bordered box five times.
        */}
        <dl className="mt-10 grid gap-x-12 gap-y-8 md:grid-cols-2 lg:mt-14 lg:gap-y-10">
          {PROMISES.map((promise) => (
            <div key={promise.title} className="border-t border-rule pt-5">
              <dt className="font-display text-h4 text-ink">{promise.title}</dt>
              <dd className="mt-2 max-w-prose text-body text-ink-soft">{promise.body}</dd>
            </div>
          ))}
        </dl>
      </div>
    </section>
  );
}
