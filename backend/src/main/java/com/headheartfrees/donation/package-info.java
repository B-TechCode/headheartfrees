/**
 * Donations. Intentionally empty.
 *
 * <p>PROJECT_BRIEF.md originally specified a {@code donation_intents} table and
 * a {@code POST /api/v1/donations/intent} endpoint. Both were removed in phase
 * 8, and the §5 note records why in full. The short version: there is no
 * payment provider and one cannot be added by a developer, so nothing could
 * ever close an intent row. A record that nothing can reconcile against is
 * write-only storage of a figure somebody typed, in a project that truncates
 * feedback timestamps and stores no IP addresses.
 *
 * <p>{@code /support} is a static page and calls no API at all.
 *
 * <p>This package is kept rather than deleted so the decision is visible where
 * someone would go looking for the code. <strong>If a provider is ever chosen,
 * do not build to the removed sketch</strong> — take the shape from that
 * provider's reconciliation model, which is what makes a stored intent mean
 * something.
 */
package com.headheartfrees.donation;
