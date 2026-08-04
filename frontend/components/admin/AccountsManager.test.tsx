import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AccountsManager } from "./AccountsManager";
import type { AdminUserAccountDto } from "@/lib/api";

vi.mock("next-intl", () => ({
  useTranslations: () => (key: string, values?: Record<string, unknown>) =>
    values ? `${key}:${JSON.stringify(values)}` : key,
}));

vi.mock("@/lib/api", () => ({
  applyAdminAccountSignupInfoToPerson: vi.fn(),
  disableAdminAccount: vi.fn(),
  deleteAdminAccount: vi.fn(),
  enableAdminAccount: vi.fn(),
  linkAdminAccount: vi.fn(),
  revokeAdminAccessForAccount: vi.fn(),
  unlinkAdminAccount: vi.fn(),
  updateAdminAccountSignupInfo: vi.fn(),
  AdminActionError: class AdminActionError extends Error {},
}));

vi.mock("./PersonPicker", () => ({
  PersonPicker: () => <div>picker</div>,
}));

function account(overrides: Partial<AdminUserAccountDto>): AdminUserAccountDto {
  return {
    id: 1,
    email: "member@example.com",
    status: "ACTIVE",
    preferredLanguage: "en",
    createdAt: "2026-01-01T00:00:00Z",
    lastLoginAt: null,
    isAdmin: false,
    linkedPersonId: null,
    linkedPersonName: null,
    submittedFullName: null,
    submittedFatherName: null,
    submittedMotherName: null,
    submittedGrandfatherName: null,
    submittedDobAd: null,
    ...overrides,
  };
}

describe("AccountsManager", () => {
  it("collapses a row to just the name and email, with actions hidden", () => {
    render(
      <AccountsManager
        initialItems={[account({ id: 1, email: "yuva@example.com", submittedFullName: "Yuva Bhatta" })]}
        currentUserEmail={null}
      />,
    );

    expect(screen.getByText("Yuva Bhatta")).toBeInTheDocument();
    expect(screen.getByText("yuva@example.com")).toBeInTheDocument();
    expect(screen.queryByText("editInfo")).not.toBeInTheDocument();
    expect(screen.queryByText("delete")).not.toBeInTheDocument();
  });

  it("expands a row on click to reveal the action buttons", () => {
    render(
      <AccountsManager
        initialItems={[account({ id: 1, email: "yuva@example.com", submittedFullName: "Yuva Bhatta" })]}
        currentUserEmail={null}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: /Yuva Bhatta/ }));

    expect(screen.getByText("editInfo")).toBeInTheDocument();
    expect(screen.getByText("delete")).toBeInTheDocument();
  });

  it("falls back to the email alone when there's no name, without repeating it", () => {
    render(<AccountsManager initialItems={[account({ id: 1, email: "noname@example.com" })]} currentUserEmail={null} />);

    expect(screen.getAllByText("noname@example.com")).toHaveLength(1);
  });

  it("filters the list to accounts matching the search box", () => {
    render(
      <AccountsManager
        initialItems={[
          account({ id: 1, email: "alice@example.com", submittedFullName: "Alice Bhatta" }),
          account({ id: 2, email: "bob@example.com", submittedFullName: "Bob Bhatta" }),
        ]}
        currentUserEmail={null}
      />,
    );

    fireEvent.change(screen.getByPlaceholderText("searchPlaceholder"), { target: { value: "alice" } });

    expect(screen.getByText("Alice Bhatta")).toBeInTheDocument();
    expect(screen.queryByText("Bob Bhatta")).not.toBeInTheDocument();
  });

  it("shows a no-results message when the search matches nothing", () => {
    render(
      <AccountsManager
        initialItems={[account({ id: 1, email: "alice@example.com", submittedFullName: "Alice Bhatta" })]}
        currentUserEmail={null}
      />,
    );

    fireEvent.change(screen.getByPlaceholderText("searchPlaceholder"), { target: { value: "nobody" } });

    expect(screen.getByText("noResults")).toBeInTheDocument();
  });
});
