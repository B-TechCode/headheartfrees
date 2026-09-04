"use client";

import { useState } from "react";
import type { ReactNode } from "react";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader, CardTitle } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { FormField } from "@/components/ui/FormField";
import { Input } from "@/components/ui/Input";
import { Label } from "@/components/ui/Label";
import { Logo, Wordmark } from "@/components/ui/Logo";
import { Spinner } from "@/components/ui/Spinner";
import { Textarea } from "@/components/ui/Textarea";

/*
 * Internal preview route.
 *
 * This is a working tool for building and reviewing the primitives, not a
 * product page — it is not linked from the navbar and it is scheduled for
 * deletion in Phase 9. It is a client component so the interactive states
 * (chip selection, the loading toggle) are real rather than mocked.
 */

const BUTTON_VARIANTS = ["primary", "secondary", "ghost", "destructive"] as const;
const BADGE_TONES = ["neutral", "accent", "success", "warning", "danger", "info"] as const;

export default function DesignSystemPage() {
  const [selectedMood, setSelectedMood] = useState<string | null>("Heavy");
  const [loading, setLoading] = useState(false);
  const [errorDemo, setErrorDemo] = useState("");

  return (
    <div className="mx-auto max-w-5xl px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
      <header className="max-w-2xl">
        <p className="font-sans text-overline font-semibold tracking-[0.085em] text-ink-soft uppercase">
          Internal
        </p>
        <h1 className="mt-3 font-display text-h1 text-ink">Design system</h1>
        <p className="mt-4 text-body-lg text-ink-soft">
          Every primitive in every state. Delete this route in Phase 9.
        </p>
      </header>

      <Section title="Brand">
        <Row label="Wordmark">
          <Wordmark />
        </Row>
        <Row label="Mark on ink">
          <span className="inline-flex items-center gap-4">
            <Logo className="h-8 w-8 text-ink" />
            <Logo className="h-8 w-8 text-clay" />
            <span className="inline-flex h-14 w-14 items-center justify-center rounded-lg bg-surface-inverse">
              <Logo className="h-8 w-8 text-ink-inverse" />
            </span>
          </span>
        </Row>
      </Section>

      <Section title="Type scale">
        <div className="flex flex-col gap-4">
          <p className="font-display text-display text-ink">Display — Fraunces</p>
          <p className="font-display text-h1 text-ink">Heading 1 — Fraunces</p>
          <p className="font-display text-h2 text-ink">Heading 2 — Fraunces</p>
          <p className="font-display text-h3 text-ink">Heading 3 — Fraunces</p>
          <p className="font-display text-h4 text-ink">Heading 4 — Fraunces</p>
          <hr className="border-rule" />
          <p className="text-body-lg text-ink">
            Body large — Karla. Used for the vent textarea and lead paragraphs.
          </p>
          <p className="text-body text-ink">
            Body — Karla. The default. Long-form reading and most UI copy sits here.
          </p>
          <p className="text-body-sm text-ink-soft">
            Body small — Karla. Labels, secondary copy, footer links.
          </p>
          <p className="text-caption text-ink-soft">
            Caption — Karla. Counters, timestamps, legal.
          </p>
          <p className="text-overline font-semibold text-ink-soft uppercase">
            Overline — Karla
          </p>
        </div>
      </Section>

      <Section title="Colour">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          <Swatch className="bg-surface border border-rule" name="surface" />
          <Swatch className="bg-surface-raised border border-rule" name="surface-raised" />
          <Swatch className="bg-surface-sunk border border-rule" name="surface-sunk" />
          <Swatch className="bg-surface-inverse" name="surface-inverse" dark />
          <Swatch className="bg-clay" name="clay" dark />
          <Swatch className="bg-clay-hover" name="clay-hover" dark />
          <Swatch className="bg-clay-active" name="clay-active" dark />
          <Swatch className="bg-clay-wash border border-rule" name="clay-wash" />
          <Swatch className="bg-success" name="success" dark />
          <Swatch className="bg-warning" name="warning" dark />
          <Swatch className="bg-danger" name="danger" dark />
          <Swatch className="bg-info" name="info" dark />
        </div>
      </Section>

      <Section title="Button — variants and states">
        {BUTTON_VARIANTS.map((variant) => (
          <Row key={variant} label={variant}>
            <div className="flex flex-wrap items-center gap-3">
              <Button variant={variant}>Default</Button>
              <Button variant={variant} disabled>
                Disabled
              </Button>
              <Button variant={variant} loading loadingLabel="Releasing">
                Loading
              </Button>
            </div>
          </Row>
        ))}

        <Row label="sizes">
          <div className="flex flex-wrap items-center gap-3">
            <Button size="sm">Small</Button>
            <Button size="md">Medium</Button>
            <Button size="lg">Large</Button>
          </div>
        </Row>

        <Row label="full width">
          <Button fullWidth>Release &amp; Let Go</Button>
        </Row>

        <Row label="live loading">
          <Button
            variant="primary"
            loading={loading}
            onClick={() => {
              setLoading(true);
              window.setTimeout(() => setLoading(false), 1600);
            }}
          >
            Press to load
          </Button>
        </Row>

        <p className="mt-2 text-body-sm text-ink-soft">
          Hover, active and focus-visible are live — tab through the row above to see the
          focus ring. Every size clears a 44px target.
        </p>
      </Section>

      <Section title="Form controls">
        <div className="grid gap-6 lg:grid-cols-2">
          <FormField label="Display name" optional hint="Shown beside your feedback.">
            {(ids) => <Input {...ids} placeholder="How should we call you?" />}
          </FormField>

          <FormField label="Email" error={errorDemo}>
            {(ids) => (
              <Input
                {...ids}
                type="email"
                placeholder="you@example.com"
                onChange={(event) =>
                  setErrorDemo(
                    event.target.value.includes("@") ? "" : "Enter a valid email address.",
                  )
                }
              />
            )}
          </FormField>
        </div>

        <p className="mt-2 text-body-sm text-ink-soft">
          Type into the email field without an <code>@</code> to see the error state and the
          live region fire.
        </p>

        <div className="mt-6 grid gap-6 lg:grid-cols-2">
          <FormField label="Disabled input">
            {(ids) => <Input {...ids} disabled value="Not editable" readOnly />}
          </FormField>
          <div className="flex flex-col gap-2">
            <Label>Standalone label</Label>
            <Input placeholder="Label used on its own" aria-label="Standalone example" />
          </div>
        </div>

        <div className="mt-6">
          <FormField
            label="What is weighing on you?"
            hint="This stays in your browser. It is never sent anywhere."
          >
            {(ids) => <Textarea {...ids} placeholder="Start anywhere." />}
          </FormField>
        </div>

        <div className="mt-6">
          <FormField label="Textarea, error state" error="Say a little more before releasing.">
            {(ids) => <Textarea {...ids} rows={3} defaultValue="…" />}
          </FormField>
        </div>
      </Section>

      <Section title="Chip — selectable">
        <div className="flex flex-wrap gap-2">
          {["Heavy", "Anxious", "Numb", "Angry", "Tired", "Lost"].map((mood) => (
            <Chip
              key={mood}
              selected={selectedMood === mood}
              onClick={() => setSelectedMood(selectedMood === mood ? null : mood)}
            >
              {mood}
            </Chip>
          ))}
          <Chip disabled>Disabled</Chip>
        </div>
        <p className="mt-3 text-body-sm text-ink-soft">
          Ships icon-less. The <code>icon</code> slot takes a custom line icon in Phase 6 —
          never an emoji.
        </p>
      </Section>

      <Section title="Badge">
        <div className="flex flex-wrap gap-2">
          {BADGE_TONES.map((tone) => (
            <Badge key={tone} tone={tone}>
              {tone}
            </Badge>
          ))}
        </div>
      </Section>

      <Section title="Card">
        <div className="grid gap-4 md:grid-cols-3">
          <Card tone="raised">
            <CardHeader>
              <CardTitle>Raised</CardTitle>
            </CardHeader>
            <CardBody>Lifts off the page. Borders and surface, never a drop shadow.</CardBody>
          </Card>
          <Card tone="sunk">
            <CardHeader>
              <CardTitle>Sunk</CardTitle>
            </CardHeader>
            <CardBody>Recedes into the page. Good for quiet, secondary content.</CardBody>
          </Card>
          <Card tone="outline">
            <CardHeader>
              <CardTitle>Outline</CardTitle>
            </CardHeader>
            <CardBody>Sits flat. A hairline rule and nothing else.</CardBody>
          </Card>
        </div>
      </Section>

      <Section title="Spinner">
        <div className="flex flex-wrap items-center gap-6 text-ink">
          <Spinner />
          <Spinner className="h-6 w-6" />
          <span className="inline-flex items-center gap-2 text-clay-hover">
            <Spinner className="h-5 w-5" />
            <span className="text-body-sm">Inherits currentColor</span>
          </span>
        </div>
      </Section>
    </div>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="mt-14 border-t border-rule pt-8">
      <h2 className="font-display text-h2 text-ink">{title}</h2>
      <div className="mt-6 flex flex-col gap-4">{children}</div>
    </section>
  );
}

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid gap-2 sm:grid-cols-[8rem_1fr] sm:items-center sm:gap-4">
      <span className="font-sans text-caption text-ink-soft">{label}</span>
      <div>{children}</div>
    </div>
  );
}

function Swatch({ className, name, dark }: { className: string; name: string; dark?: boolean }) {
  return (
    <div className="overflow-hidden rounded-md border border-rule">
      <div className={`${className} flex h-16 items-end p-2`}>
        <span className={dark ? "text-caption text-ink-inverse" : "text-caption text-ink"}>
          {name}
        </span>
      </div>
    </div>
  );
}
