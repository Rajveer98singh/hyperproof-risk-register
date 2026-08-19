import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { SeverityBadge } from "./SeverityBadge";

describe("SeverityBadge", () => {
  it("shows a text label (not color alone) and the score", () => {
    render(<SeverityBadge severity="CRITICAL" score={25} />);
    expect(screen.getByText(/Critical/)).toBeInTheDocument();
    expect(screen.getByText(/25/)).toBeInTheDocument();
  });
});
