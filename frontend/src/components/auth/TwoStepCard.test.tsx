import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { TwoStepCard } from "@/components/auth/TwoStepCard";
import { SessionProvider } from "@/lib/auth/SessionProvider";
import { aUser, jsonResponse, stubFetch } from "@/test/http";
import { resetNavigation } from "@/test/navigation";
import type { UserSummary } from "@/lib/auth/types";

/**
 * The account page's second-factor card.
 *
 * Two properties are worth holding here and the rest is layout:
 *
 *   1. **Setup does not enable.** The QR appears and nothing is on until a
 *      code has been verified. A card that flipped to "On" as soon as the
 *      secret was shown would be lying, and the person would find out at their
 *      next sign-in.
 *   2. **An admin gets no off switch.** The server refuses it with a 403, so
 *      a button here would be offering something that cannot happen.
 */
const SETUP = {
  manualKey: "ABCD EFGH IJKL MNOP",
  otpauthUri: "otpauth://totp/HeadHeartFreeS:person@example.com?secret=ABCDEFGHIJKLMNOP",
  qrDataUri: "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=",
};

function renderCard(user: Partial<UserSummary>, onChanged = vi.fn()) {
  return render(
    <SessionProvider>
      <TwoStepCard user={aUser(user) as UserSummary} onChanged={onChanged} />
    </SessionProvider>,
  );
}

describe("two-step sign-in on the account page", () => {
  beforeEach(() => {
    resetNavigation();
    document.cookie = "hhf_seen=; Max-Age=0; path=/";
  });

  it("offers setup to a USER, as an option rather than a demand", async () => {
    stubFetch((url) => {
      throw new Error(`Unexpected request to ${url}`);
    });

    renderCard({ totpEnabled: false, totpRequired: false });

    expect(screen.getByText(/optional/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /set it up/i })).toBeInTheDocument();
  });

  it("shows the QR and the manual key, and does not turn anything on yet", async () => {
    const user = userEvent.setup();
    const fetchMock = stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/totp/setup")) return jsonResponse(200, SETUP);
      throw new Error(`Unexpected request to ${url}`);
    });

    const onChanged = vi.fn();
    renderCard({ totpEnabled: false }, onChanged);
    await user.click(screen.getByRole("button", { name: /set it up/i }));

    // Both forms of the same secret. The manual key is not a fallback for the
    // QR - it is the accessible path, and the only one that works when you are
    // reading this on the phone that holds the authenticator.
    expect(await screen.findByText(SETUP.manualKey)).toBeInTheDocument();
    const qr = document.querySelector("img");
    expect(qr).toHaveAttribute("src", SETUP.qrDataUri);
    // alt="" on purpose: it is a picture of the string printed beside it, and
    // an alt of "QR code" announces a dead end to a screen reader.
    expect(qr).toHaveAttribute("alt", "");

    // Nothing has been enabled, and nothing told the page otherwise.
    expect(onChanged).not.toHaveBeenCalled();
    expect(
      fetchMock.mock.calls.filter((c) => String(c[0]).includes("/enable")),
    ).toHaveLength(0);
  });

  it("requires a verified code before enabling, then shows the codes once", async () => {
    const user = userEvent.setup();
    const fetchMock = stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/totp/setup")) return jsonResponse(200, SETUP);
      if (url.endsWith("/api/v1/auth/totp/enable")) {
        return jsonResponse(200, { backupCodes: ["AB2CD-3EFGH", "JK4MN-5PQRS"] });
      }
      throw new Error(`Unexpected request to ${url}`);
    });

    renderCard({ totpEnabled: false });
    await user.click(screen.getByRole("button", { name: /set it up/i }));
    await screen.findByText(SETUP.manualKey);

    // The button is dead until there are six digits, so "enable without
    // verifying" is not reachable from the UI at all.
    const enable = screen.getByRole("button", { name: /turn on two-step sign-in/i });
    expect(enable).toBeDisabled();

    await user.type(screen.getByLabelText(/six-digit code/i), "123456");
    expect(enable).toBeEnabled();
    await user.click(enable);

    expect(await screen.findByText("AB2CD-3EFGH")).toBeInTheDocument();
    expect(screen.getByText(/you will not see them again/i)).toBeInTheDocument();

    const call = fetchMock.mock.calls.find((c) => String(c[0]).endsWith("/enable"));
    expect(JSON.parse(String((call?.[1] as RequestInit).body))).toEqual({ code: "123456" });
  });

  it("gives an admin no way to turn it off", async () => {
    stubFetch((url) => {
      throw new Error(`Unexpected request to ${url}`);
    });

    renderCard({ role: "ADMIN", totpEnabled: true, totpRequired: true, backupCodesRemaining: 10 });

    expect(screen.queryByRole("button", { name: /turn it off/i })).not.toBeInTheDocument();
    expect(screen.getByText(/cannot be turned off on an admin account/i)).toBeInTheDocument();
    // Regenerating is still offered - an admin running low on recovery codes
    // needs that more than anyone.
    expect(screen.getByRole("button", { name: /new backup codes/i })).toBeInTheDocument();
  });

  it("lets a USER turn it off, and asks for the password as well as a code", async () => {
    const user = userEvent.setup();
    stubFetch((url) => {
      throw new Error(`Unexpected request to ${url}`);
    });

    renderCard({ totpEnabled: true, totpRequired: false, backupCodesRemaining: 9 });
    await user.click(screen.getByRole("button", { name: /turn it off/i }));

    // The password, and not only the session. The threat here is somebody else
    // holding the session - and a session is exactly what they have.
    expect(screen.getByLabelText("Password")).toBeInTheDocument();
    expect(screen.getByLabelText(/six-digit code/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /turn it off/i })).toBeDisabled();
  });

  it("warns when the backup codes are running out", () => {
    stubFetch((url) => {
      throw new Error(`Unexpected request to ${url}`);
    });

    renderCard({ totpEnabled: true, backupCodesRemaining: 2 });

    // This is the only place anybody would ever find out. Somebody who has
    // used eight of ten has no other signal until the day it matters.
    expect(screen.getByText(/only 2 backup codes left/i)).toBeInTheDocument();
  });

  it("says so plainly when there are none left", () => {
    stubFetch((url) => {
      throw new Error(`Unexpected request to ${url}`);
    });

    renderCard({ totpEnabled: true, backupCodesRemaining: 0 });

    expect(screen.getByText(/no backup codes left/i)).toBeInTheDocument();
  });
});
