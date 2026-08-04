import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SignupForm } from "./SignupForm";

vi.mock("next-intl", () => ({
  useLocale: () => "en",
  useTranslations: () => {
    const t = (key: string) => key;
    t.rich = (key: string, values: Record<string, (chunks: React.ReactNode) => React.ReactNode>) => (
      <>
        {key}
        {Object.entries(values).map(([name, render]) => (
          <span key={name}>{render(name)}</span>
        ))}
      </>
    );
    return t;
  },
}));

vi.mock("@/i18n/navigation", () => ({
  Link: ({ children, href }: { children: React.ReactNode; href: string }) => <a href={href}>{children}</a>,
  usePathname: () => "/signup",
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock("@/i18n/routing", () => ({
  routing: { locales: ["en", "ne"] },
}));

vi.mock("@/lib/api", () => ({
  convertAdToBs: vi.fn().mockRejectedValue(new Error("unsupported")),
  convertBsToAd: vi.fn().mockRejectedValue(new Error("unsupported")),
  submitSignup: vi.fn(),
  SignupError: class SignupError extends Error {},
}));

describe("SignupForm", () => {
  it("marks Full Name, Email, Father's, and Grandfather's names as required", () => {
    render(<SignupForm />);

    expect(screen.getByText("fields.fullName").parentElement).toHaveTextContent("fields.fullName*");
    expect(screen.getByText("fields.email").parentElement).toHaveTextContent("fields.email*");
    expect(screen.getByText("fields.fatherName").parentElement).toHaveTextContent("fields.fatherName*");
    expect(screen.getByText("fields.grandfatherName").parentElement).toHaveTextContent("fields.grandfatherName*");
  });

  it("shows validation errors for the required fields when submitted empty", () => {
    render(<SignupForm />);

    fireEvent.click(screen.getByRole("button", { name: "submit" }));

    expect(screen.getByText("errors.fullNameRequired")).toBeInTheDocument();
    expect(screen.getByText("errors.emailRequired")).toBeInTheDocument();
    expect(screen.getByText("errors.fatherNameRequired")).toBeInTheDocument();
    expect(screen.getByText("errors.grandfatherNameRequired")).toBeInTheDocument();
  });

  it("no longer renders the removed Family Branch, Relative Name, or Invitation Code fields", () => {
    render(<SignupForm />);

    expect(screen.queryByText("fields.familyBranch")).not.toBeInTheDocument();
    expect(screen.queryByText("fields.knownRelativeName")).not.toBeInTheDocument();
    expect(screen.queryByText("fields.invitationCode")).not.toBeInTheDocument();
  });

  it("no longer renders the subtitle", () => {
    render(<SignupForm />);

    expect(screen.queryByText("subtitle")).not.toBeInTheDocument();
  });
});
