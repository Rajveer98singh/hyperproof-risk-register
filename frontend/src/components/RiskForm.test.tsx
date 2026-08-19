import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RiskForm } from "./RiskForm";

describe("RiskForm live inherent score", () => {
  it("recomputes the inherent score badge as likelihood/impact change", async () => {
    const user = userEvent.setup();
    render(
      <RiskForm frameworks={[]} submitLabel="Create" onSubmit={vi.fn().mockResolvedValue(undefined)} />,
    );

    // Defaults 3 x 3 = 9 (Medium).
    expect(screen.getByText(/Medium · 9/)).toBeInTheDocument();

    const [likelihood, impact] = screen.getAllByRole("combobox").slice(-2);
    // Set 5 x 5 = 25 (Critical).
    await user.selectOptions(likelihood, "5");
    await user.selectOptions(impact, "5");

    expect(screen.getByText(/Critical · 25/)).toBeInTheDocument();
  });

  it("surfaces server field errors inline", async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn().mockRejectedValue({
      name: "ApiError",
      status: 400,
      detail: "Validation failed",
      fieldErrors: { title: "must not be blank" },
    });
    render(<RiskForm frameworks={[]} submitLabel="Create" onSubmit={onSubmit} />);

    await user.click(screen.getByRole("button", { name: /Create/ }));

    expect(await screen.findByText("must not be blank")).toBeInTheDocument();
  });
});
