import { describe, expect, it } from "vitest";
import { severityForScore, severityLabel, SEVERITY_COLORS } from "./severity";

describe("severityForScore band boundaries", () => {
  it.each([
    [1, "LOW"],
    [5, "LOW"],
    [6, "MEDIUM"],
    [12, "MEDIUM"],
    [13, "HIGH"],
    [19, "HIGH"],
    [20, "CRITICAL"],
    [25, "CRITICAL"],
  ])("score %i -> %s", (score, expected) => {
    expect(severityForScore(score)).toBe(expected);
  });
});

describe("severity presentation", () => {
  it("labels are title-cased", () => {
    expect(severityLabel("CRITICAL")).toBe("Critical");
  });
  it("every severity has a color", () => {
    (["LOW", "MEDIUM", "HIGH", "CRITICAL"] as const).forEach((s) => {
      expect(SEVERITY_COLORS[s].bg).toMatch(/^#/);
    });
  });
});
