import type { Severity } from "../types";

// Mirrors the backend severity bands (Low 1-5, Medium 6-12, High 13-19, Critical 20-25).
// Kept on the client so the create/edit form can show a live severity badge for the inherent
// score without a server round-trip. If the bands change, update both sides.
export function severityForScore(score: number): Severity {
  if (score >= 20) return "CRITICAL";
  if (score >= 13) return "HIGH";
  if (score >= 6) return "MEDIUM";
  return "LOW";
}

// Single source of truth for severity colors. Text label is always shown alongside the color so
// the UI is readable for color-blind users (not color-only).
export const SEVERITY_COLORS: Record<Severity, { bg: string; fg: string }> = {
  LOW: { bg: "#1b7f4b", fg: "#ffffff" },
  MEDIUM: { bg: "#b7791f", fg: "#ffffff" },
  HIGH: { bg: "#c05621", fg: "#ffffff" },
  CRITICAL: { bg: "#c0341d", fg: "#ffffff" },
};

export function severityLabel(sev: Severity): string {
  return sev.charAt(0) + sev.slice(1).toLowerCase();
}
