import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { TreeFilters } from "./TreeFilters";

vi.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
}));

describe("TreeFilters", () => {
  it("renders just the search box, with no clear button when empty", () => {
    render(<TreeFilters search="" onSearchChange={vi.fn()} />);

    expect(screen.getByRole("searchbox")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("reports typed input back to the caller", () => {
    const onSearchChange = vi.fn();
    render(<TreeFilters search="" onSearchChange={onSearchChange} />);

    fireEvent.change(screen.getByRole("searchbox"), { target: { value: "Ram" } });

    expect(onSearchChange).toHaveBeenCalledWith("Ram");
  });

  it("clears the search when the clear button is clicked", () => {
    const onSearchChange = vi.fn();
    render(<TreeFilters search="Ram" onSearchChange={onSearchChange} />);

    fireEvent.click(screen.getByRole("button"));

    expect(onSearchChange).toHaveBeenCalledWith("");
  });
});
