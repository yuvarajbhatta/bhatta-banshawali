import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { VerifyEmailAction } from "./VerifyEmailAction";
import { confirmEmailVerification } from "@/lib/api";

vi.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

vi.mock("@/i18n/navigation", () => ({
  Link: ({ children, href }: { children: React.ReactNode; href: string }) => <a href={href}>{children}</a>,
}));

vi.mock("@/lib/api", () => ({
  confirmEmailVerification: vi.fn(),
  EmailVerificationError: class EmailVerificationError extends Error {},
}));

describe("VerifyEmailAction", () => {
  it("does not confirm on mount -- only an explicit click fires the request", () => {
    // The whole point of this component: some corporate email-security
    // scanners pre-fetch every link in an email before a human clicks it.
    // If loading this page auto-confirmed, a scanner would burn the
    // single-use token before the real user gets to it.
    render(<VerifyEmailAction token="raw-token" />);

    expect(confirmEmailVerification).not.toHaveBeenCalled();
  });

  it("confirms only after the button is clicked", async () => {
    vi.mocked(confirmEmailVerification).mockResolvedValue({ status: "EMAIL_VERIFIED" });

    render(<VerifyEmailAction token="raw-token" />);
    fireEvent.click(screen.getByRole("button", { name: "confirmButton" }));

    await waitFor(() => expect(confirmEmailVerification).toHaveBeenCalledWith("raw-token"));
    expect(confirmEmailVerification).toHaveBeenCalledTimes(1);
    expect(await screen.findByText("successMessage")).toBeInTheDocument();
  });
});
