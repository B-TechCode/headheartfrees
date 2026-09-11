import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { LoginForm } from "@/components/auth/LoginForm";
import { SessionProvider } from "@/lib/auth/SessionProvider";
import {
  aTotpChallenge,
  anAuthenticatedLogin,
  anEnrolmentChallenge,
  invalidTotpCode,
  jsonResponse,
  requestedUrls,
  stubFetch,
} from "@/test/http";
import { resetNavigation, routerPush, routerReplace } from "@/test/navigation";

/**
 * Signing in when a code is owed.
 *
 * The tests that matter here are the negative ones. A component that showed a
 * code field and then signed the person in anyway would look completely
 * correct in a screenshot, so what is asserted is what must NOT happen: no
 * navigation, and no request to any endpoint that would use a session, until a
 * code has actually been accepted.
 */
describe("the code step", () => {
  beforeEach(() => {
    resetNavigation();
    // No session hint, so SessionProvider makes no restore call on mount and
    // every request these tests see is one the form deliberately made.
    document.cookie = "hhf_seen=; Max-Age=0; path=/";
  });

  function renderLogin() {
    return render(
      <SessionProvider>
        <LoginForm />
      </SessionProvider>,
    );
  }

  async function submitPassword(user: ReturnType<typeof userEvent.setup>) {
    await user.type(screen.getByLabelText("Email"), "admin@example.com");
    await user.type(screen.getByLabelText("Password"), "a quiet long passphrase");
    await user.click(screen.getByRole("button", { name: /^sign in$/i }));
  }

  it("asks for a code instead of signing in, when the account has one", async () => {
    const user = userEvent.setup();
    const fetchMock = stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/login")) return jsonResponse(200, aTotpChallenge());
      throw new Error(`Unexpected request to ${url}`);
    });

    renderLogin();
    await submitPassword(user);

    // The code field is there...
    expect(await screen.findByLabelText(/six-digit code/i)).toBeInTheDocument();
    // ...and nothing else happened. No redirect, and no second request - in
    // particular nothing that would have carried or produced a session.
    expect(routerPush).not.toHaveBeenCalled();
    expect(routerReplace).not.toHaveBeenCalled();
    expect(requestedUrls(fetchMock)).toEqual(["http://localhost:8080/api/v1/auth/login"]);
  });

  it("sends the ticket and the code, and signs in when the server accepts", async () => {
    const user = userEvent.setup();
    const fetchMock = stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/login")) return jsonResponse(200, aTotpChallenge("T-1"));
      if (url.endsWith("/api/v1/auth/login/totp")) {
        return jsonResponse(200, anAuthenticatedLogin());
      }
      throw new Error(`Unexpected request to ${url}`);
    });

    renderLogin();
    await submitPassword(user);

    await user.type(await screen.findByLabelText(/six-digit code/i), "123456");
    await user.click(screen.getByRole("button", { name: /continue/i }));

    const call = fetchMock.mock.calls.find((c) => String(c[0]).endsWith("/login/totp"));
    expect(JSON.parse(String((call?.[1] as RequestInit).body))).toEqual({
      ticket: "T-1",
      code: "123456",
    });

    await vi.waitFor(() => expect(routerReplace).toHaveBeenCalledWith("/"));
  });

  it("shows the API's own message on a wrong code, and does not sign in", async () => {
    const user = userEvent.setup();
    stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/login")) return jsonResponse(200, aTotpChallenge());
      if (url.endsWith("/api/v1/auth/login/totp")) return invalidTotpCode();
      throw new Error(`Unexpected request to ${url}`);
    });

    renderLogin();
    await submitPassword(user);

    await user.type(await screen.findByLabelText(/six-digit code/i), "000000");
    await user.click(screen.getByRole("button", { name: /continue/i }));

    // The server's sentence, rendered as-is. A friendlier rewrite here would
    // undo, in the layer nobody audits, the property the backend went to
    // trouble to have: that nobody can tell which part of an attempt failed.
    expect(await screen.findByText(/that code is not valid/i)).toBeInTheDocument();

    expect(routerPush).not.toHaveBeenCalled();
    expect(routerReplace).not.toHaveBeenCalled();
  });

  it("clears the field after a failure", async () => {
    const user = userEvent.setup();
    stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/login")) return jsonResponse(200, aTotpChallenge());
      if (url.endsWith("/api/v1/auth/login/totp")) return invalidTotpCode();
      throw new Error(`Unexpected request to ${url}`);
    });

    renderLogin();
    await submitPassword(user);

    const field = await screen.findByLabelText(/six-digit code/i);
    await user.type(field, "000000");
    await user.click(screen.getByRole("button", { name: /continue/i }));

    await screen.findByText(/that code is not valid/i);
    // The six digits that just failed are worthless either way: wrong stays
    // wrong, and right has now been spent. Leaving them invites a resubmit
    // that cannot succeed.
    expect(field).toHaveValue("");
  });

  it("takes a pasted code with spaces or dashes in it", async () => {
    const user = userEvent.setup();
    stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/login")) return jsonResponse(200, aTotpChallenge());
      throw new Error(`Unexpected request to ${url}`);
    });

    renderLogin();
    await submitPassword(user);

    const field = await screen.findByLabelText(/six-digit code/i);
    field.focus();
    await user.paste("123 456");

    expect(field).toHaveValue("123456");
    expect(screen.getByRole("button", { name: /continue/i })).toBeEnabled();
  });

  it("will not submit fewer than six digits", async () => {
    const user = userEvent.setup();
    stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/login")) return jsonResponse(200, aTotpChallenge());
      throw new Error(`Unexpected request to ${url}`);
    });

    renderLogin();
    await submitPassword(user);

    await user.type(await screen.findByLabelText(/six-digit code/i), "123");
    expect(screen.getByRole("button", { name: /continue/i })).toBeDisabled();
  });

  it("offers a backup code as a separate, unfiltered field", async () => {
    const user = userEvent.setup();
    const fetchMock = stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/login")) return jsonResponse(200, aTotpChallenge("T-2"));
      if (url.endsWith("/api/v1/auth/login/totp")) {
        return jsonResponse(200, anAuthenticatedLogin());
      }
      throw new Error(`Unexpected request to ${url}`);
    });

    renderLogin();
    await submitPassword(user);

    await user.click(await screen.findByRole("button", { name: /use a backup code/i }));

    // Its own field, because the six-digit filter would eat a backup code
    // alive - it is ten base32 characters and a dash.
    const field = screen.getByLabelText(/backup code/i);
    await user.type(field, "AB2CD-3EFGH");
    expect(field).toHaveValue("AB2CD-3EFGH");

    await user.click(screen.getByRole("button", { name: /continue/i }));

    const call = fetchMock.mock.calls.find((c) => String(c[0]).endsWith("/login/totp"));
    expect(JSON.parse(String((call?.[1] as RequestInit).body)).code).toBe("AB2CD-3EFGH");
  });

  it("sends an admin with no second factor to set one up, not to a session", async () => {
    const user = userEvent.setup();
    stubFetch((url) => {
      if (url.endsWith("/api/v1/auth/login")) return jsonResponse(200, anEnrolmentChallenge());
      if (url.endsWith("/api/v1/auth/totp/setup")) {
        return jsonResponse(200, {
          manualKey: "ABCD EFGH IJKL MNOP",
          otpauthUri: "otpauth://totp/HeadHeartFreeS:admin@example.com?secret=ABCD",
          qrDataUri: "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=",
        });
      }
      throw new Error(`Unexpected request to ${url}`);
    });

    renderLogin();
    await submitPassword(user);

    // The screen with a QR code on it, reached with a correct password and no
    // session. This is what stops the requirement locking out an admin who
    // existed before it shipped.
    expect(await screen.findByText(/set this up to continue/i)).toBeInTheDocument();
    expect(await screen.findByText("ABCD EFGH IJKL MNOP")).toBeInTheDocument();

    expect(routerPush).not.toHaveBeenCalled();
    expect(routerReplace).not.toHaveBeenCalled();
  });
});
