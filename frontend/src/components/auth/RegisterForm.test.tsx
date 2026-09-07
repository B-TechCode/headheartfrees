import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("next/navigation", async () => (await import("@/test/navigation")).navigationModule());

import { RegisterForm } from "@/components/auth/RegisterForm";
import { jsonResponse, stubFetch, tooManyRequests } from "@/test/http";
import { resetNavigation } from "@/test/navigation";

/**
 * The registration success screen, which is the part of this form that carries
 * a decision rather than a layout.
 *
 * Registration answers a duplicate address exactly as it answers a new one, so
 * that nobody can use this site to find out whether a given person has an
 * account here. The cost is that someone who has forgotten their account is
 * told it worked. The agreed mitigation — recorded in the phase 5 log before
 * this frontend existed — is that the screen renders the API's own sentence,
 * which is true either way, and puts a link to sign in beside it.
 *
 * These tests hold that. A later change that replaces the message with a
 * cheerful one of its own, or drops the link, fails here.
 */
const SHARED_MESSAGE =
  "If that address is new, your account is ready. If you already had one, sign in instead.";

describe("registering", () => {
  beforeEach(() => {
    resetNavigation();
  });

  async function fillAndSubmit(user: ReturnType<typeof userEvent.setup>) {
    await user.type(screen.getByLabelText("Email"), "person@example.com");
    await user.type(screen.getByLabelText("Password"), "a quiet long passphrase");
    await user.click(screen.getByRole("button", { name: /create account/i }));
  }

  it("shows the API's own message, and a way to sign in", async () => {
    const user = userEvent.setup();
    stubFetch(() => jsonResponse(201, { message: SHARED_MESSAGE }));

    render(<RegisterForm />);
    await fillAndSubmit(user);

    expect(await screen.findByText(SHARED_MESSAGE)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /go to sign in/i })).toHaveAttribute(
      "href",
      "/login",
    );
    // The form is gone: leaving it under the message invites a second submit
    // that would tell them nothing new.
    expect(screen.queryByRole("button", { name: /create account/i })).not.toBeInTheDocument();
  });

  it("sends null rather than an empty display name", async () => {
    const user = userEvent.setup();
    const fetchMock = stubFetch(() => jsonResponse(201, { message: SHARED_MESSAGE }));

    render(<RegisterForm />);
    await fillAndSubmit(user);

    const body = JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body));
    expect(body).toEqual({
      email: "person@example.com",
      password: "a quiet long passphrase",
      // The column is nullable; an empty string would be a display name of
      // nothing, which then has to be special-cased everywhere it is rendered.
      displayName: null,
    });
  });

  it("puts a weak-password rejection under the password field", async () => {
    const user = userEvent.setup();
    stubFetch(() =>
      jsonResponse(400, {
        timestamp: new Date().toISOString(),
        status: 400,
        code: "WEAK_PASSWORD",
        message: "Password must be at least 12 characters.",
        path: "/api/v1/auth/register",
      }),
    );

    render(<RegisterForm />);
    await fillAndSubmit(user);

    // A 400 with a sentence and no fieldErrors, because the rule that rejected
    // it is one sentence rather than a bean validation failure. It still
    // belongs beside the field a person would look at.
    const message = await screen.findByText("Password must be at least 12 characters.");
    expect(message).toBeInTheDocument();
    expect(screen.getByLabelText("Password")).toHaveAttribute("aria-invalid", "true");
  });

  it("turns a rate limit into a wait a person can act on", async () => {
    const user = userEvent.setup();
    stubFetch(() => tooManyRequests(45));

    render(<RegisterForm />);
    await fillAndSubmit(user);

    // The number is readable only because SecurityConfig exposes Retry-After
    // across origins. Without that line this degrades to the vaguer sentence.
    expect(await screen.findByText(/try again in 45 seconds/i)).toBeInTheDocument();
  });
});
