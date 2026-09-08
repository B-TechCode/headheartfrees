import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

/**
 * The support page's two states, and the things it must never grow.
 *
 * ===========================================================================
 * Why the placeholder is worth a test
 * ===========================================================================
 *
 * There are two constants to change before this page can take money, and they
 * fail in opposite directions if only one is changed. A real UPI ID with the
 * flag still `true` hides the payment details, so nobody can give anything. A
 * cleared flag with the placeholder ID still in place publishes
 * `REPLACE-ME@example.invalid` as a payment address — and a valid-but-wrong
 * VPA reaches a stranger, so that direction loses somebody's money.
 *
 * Neither failure is visible in a diff of one line. These tests are.
 *
 * ===========================================================================
 * The copy assertions
 * ===========================================================================
 *
 * The last block asserts absences. That is normally a weak kind of test, but
 * the instruction for this page was mostly a list of things it must not do,
 * and the ways it gets subtly wrong are additive: somebody adds "if you can
 * spare it" to warm it up, or a suggested amount to be helpful. Each is a
 * small edit that passes review and changes what the page is.
 */
describe("the support page", () => {
  afterEach(() => {
    vi.resetModules();
    vi.doUnmock("@/lib/support");
  });

  async function renderWith(overrides: Record<string, unknown>) {
    vi.doMock("@/lib/support", () => ({
      SUPPORT_UPI_ID: "REPLACE-ME@example.invalid",
      SUPPORT_PAYMENT_IS_PLACEHOLDER: true,
      SUPPORT_PAYEE_NAME: "HeadHeartFreeS",
      SUPPORT_UPI_LINK: "upi://pay?pa=test%40upi&pn=HeadHeartFreeS&cu=INR",
      ...overrides,
    }));
    vi.resetModules();
    const { default: SupportPage } = await import("@/app/support/page");
    return render(<SupportPage />);
  }

  it("says nothing can be given yet while the payment method is a placeholder", async () => {
    await renderWith({ SUPPORT_PAYMENT_IS_PLACEHOLDER: true });

    // Worded for a visitor, not as a status line about the software.
    expect(screen.getByText(/no way to give anything yet/i)).toBeInTheDocument();
    expect(screen.getByText(/that is fine/i)).toBeInTheDocument();
  });

  it("shows no payment address at all while the flag is set", async () => {
    await renderWith({ SUPPORT_PAYMENT_IS_PLACEHOLDER: true });

    // The failure this catches: clearing the ID's guard but not the flag, or
    // rendering the details above the notice by accident.
    expect(screen.queryByText(/REPLACE-ME@example.invalid/)).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: /upi app/i })).not.toBeInTheDocument();
  });

  it("shows the ID and the link once a real method is configured", async () => {
    await renderWith({
      SUPPORT_PAYMENT_IS_PLACEHOLDER: false,
      SUPPORT_UPI_ID: "someone@examplebank",
      SUPPORT_UPI_LINK: "upi://pay?pa=someone%40examplebank&pn=HeadHeartFreeS&cu=INR",
    });

    expect(screen.getByText("someone@examplebank")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /upi app/i })).toHaveAttribute(
      "href",
      expect.stringContaining("upi://pay"),
    );
    expect(screen.queryByText(/no way to give anything yet/i)).not.toBeInTheDocument();
  });

  it("never prefills an amount in the payment link", async () => {
    await renderWith({
      SUPPORT_PAYMENT_IS_PLACEHOLDER: false,
      SUPPORT_UPI_ID: "someone@examplebank",
      SUPPORT_UPI_LINK: "upi://pay?pa=someone%40examplebank&pn=HeadHeartFreeS&cu=INR",
    });

    // `am=` on a upi:// link prefills a figure in the payment app, which is a
    // suggested amount presented as a default — the thing this page is not
    // allowed to do, arriving through the back door.
    const href = screen.getByRole("link", { name: /upi app/i }).getAttribute("href") ?? "";
    expect(href).not.toMatch(/[?&]am=/);
  });

  it("states that the site is free and that giving buys nothing", async () => {
    await renderWith({});

    expect(screen.getByText(/it stays free/i)).toBeInTheDocument();
    expect(screen.getByText(/no tier, no membership/i)).toBeInTheDocument();
  });

  it("carries no goal, no total, no urgency and no suggested amount", async () => {
    const { container } = await renderWith({});
    const text = container.textContent ?? "";

    // Each of these is a specific thing the brief ruled out, and each is the
    // kind of phrase that gets added later to warm the page up.
    for (const forbidden of [
      /goal/i,
      /progress/i,
      /raised so far/i,
      /help us reach/i,
      /we need/i,
      /survive/i,
      /if you can spare/i,
      /every little/i,
      /suggested/i,
      /₹\s*\d/,
    ]) {
      expect(text, `"${forbidden}" must not appear on the support page`).not.toMatch(forbidden);
    }
  });

  it("makes no claim about tax status", async () => {
    const { container } = await renderWith({});
    const text = container.textContent ?? "";

    // Nothing here may imply a registration that does not exist. See
    // HANDOVER.md 2.8 — it is a misrepresentation to the person giving.
    for (const forbidden of [/tax.?deductible/i, /80G/i, /charity/i, /nonprofit/i, /receipt/i]) {
      expect(text, `"${forbidden}" implies a tax status nobody has established`).not.toMatch(
        forbidden,
      );
    }
  });
});
