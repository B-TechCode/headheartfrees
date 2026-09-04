/**
 * Joins class names, dropping anything falsy.
 *
 * Deliberately not `clsx` + `tailwind-merge`: the variant maps in
 * `components/ui` are exhaustive rather than additive, so there are no
 * conflicting utilities to resolve, and two dependencies to render a string is
 * not a trade worth making. If a component ever needs genuine conflict
 * resolution, that is the moment to reach for tailwind-merge — not before.
 */
export type ClassValue = string | number | null | undefined | false;

export function cn(...values: ClassValue[]): string {
  return values.filter(Boolean).join(" ");
}
