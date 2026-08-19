import { describe, expect, it } from "vitest";
import { inherentScore } from "./scoring";

describe("inherentScore", () => {
  it("is likelihood times impact", () => {
    expect(inherentScore(5, 5)).toBe(25);
    expect(inherentScore(1, 1)).toBe(1);
    expect(inherentScore(3, 4)).toBe(12);
  });
});
