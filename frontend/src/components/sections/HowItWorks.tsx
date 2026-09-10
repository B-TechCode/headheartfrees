import { cn } from "@/lib/cn";
import { SectionRule } from "@/components/ui/SectionRule";

/**
 * Three steps.
 *
 * The job here is to remove uncertainty about what happens when the button is
 * pressed. Someone about to type something they have not said out loud needs to
 * know, before they start, that it is not going to be sent anywhere. So step
 * three is specific about the mechanism rather than gesturing at it.
 *
 * The vertical rhythm is deliberately not uniform with the section below it —
 * §8 rules out identical spacing between every section.
 */
const STEPS = [
  {
    n: "01",
    title: "Arrive",
    body: "No account, no email, no waiting. Open the page and the box is there.",
  },
  {
    n: "02",
    title: "Write",
    body: "Type whatever is sitting on you. There is no prompt to answer and no length to hit. Nobody is going to read it, so it does not have to make sense.",
  },
  {
    n: "03",
    title: "Release",
    body: "Press the button and the text is cleared from the page. It was only ever in your browser, so there is nothing to delete and nothing to recall.",
  },
] as const;

export function HowItWorks() {
  return (
    <section aria-labelledby="how-heading" className="border-b border-rule bg-surface">
      <div className="mx-auto max-w-6xl px-4 py-14 sm:px-6 lg:px-8 lg:py-20">
        <SectionRule />

        <h2 id="how-heading" className="mt-6 font-display text-h2 text-ink">
          How it works
        </h2>

        <ol className="mt-8 grid gap-8 sm:grid-cols-3 sm:gap-6 lg:mt-10 lg:gap-10">
          {STEPS.map((step) => (
            <li key={step.n} className="flex flex-col">
              <span
                aria-hidden="true"
                className="font-display text-h4 text-(--color-text-accent)"
              >
                {step.n}
              </span>
              <span aria-hidden="true" className="mt-3 block h-px w-full bg-rule" />
              <h3 className={cn("mt-4 font-display text-h4 text-ink")}>{step.title}</h3>
              <p className="mt-2 text-body text-ink-soft">{step.body}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}
