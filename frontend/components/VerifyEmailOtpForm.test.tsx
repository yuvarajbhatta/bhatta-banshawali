import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { VerifyEmailOtpForm } from "./VerifyEmailOtpForm";
import { confirmEmailVerification, resendEmailVerification } from "@/lib/api";

vi.mock("next-intl", () => ({
  useTranslations: () => (key: string, values?: Record<string, unknown>) =>
    values ? `${key}:${JSON.stringify(values)}` : key,
}));

vi.mock("@/i18n/navigation", () => ({
  Link: ({ children, href }: { children: React.ReactNode; href: string }) => <a href={href}>{children}</a>,
}));

vi.mock("@/lib/api", () => ({
  confirmEmailVerification: vi.fn(),
  resendEmailVerification: vi.fn(),
  EmailVerificationError: class EmailVerificationError extends Error {},
}));

describe("VerifyEmailOtpForm", () => {
  it("does not confirm until the form is submitted", () => {
    render(<VerifyEmailOtpForm email="a@example.com" />);
    expect(confirmEmailVerification).not.toHaveBeenCalled();
  });

  it("submits the code entered by the user", async () => {
    vi.mocked(confirmEmailVerification).mockResolvedValue({ status: "EMAIL_VERIFIED" });

    render(<VerifyEmailOtpForm email="a@example.com" />);
    fireEvent.change(screen.getByLabelText("codeLabel"), { target: { value: "123456" } });
    fireEvent.click(screen.getByRole("button", { name: "confirmButton" }));

    await waitFor(() => expect(confirmEmailVerification).toHaveBeenCalledWith("a@example.com", "123456"));
    expect(await screen.findByText("successMessage")).toBeInTheDocument();
  });

  it("calls resend and starts a cooldown", async () => {
    vi.mocked(resendEmailVerification).mockResolvedValue({ status: "CODE_SENT" });

    render(<VerifyEmailOtpForm email="a@example.com" />);
    fireEvent.click(screen.getByRole("button", { name: "resendButton" }));

    await waitFor(() => expect(resendEmailVerification).toHaveBeenCalledWith("a@example.com"));
    expect(await screen.findByText("resendSuccess")).toBeInTheDocument();
  });
});
