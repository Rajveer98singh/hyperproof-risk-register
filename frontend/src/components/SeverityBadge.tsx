import type { Severity } from "../types";
import { SEVERITY_COLORS, severityLabel } from "../lib/severity";

interface Props {
  severity: Severity;
  score?: number;
}

/** Single source of truth for how severity is shown: a colored pill with an always-visible label. */
export function SeverityBadge({ severity, score }: Props) {
  const c = SEVERITY_COLORS[severity];
  return (
    <span
      className="badge"
      style={{ backgroundColor: c.bg, color: c.fg }}
      title={`${severityLabel(severity)} severity`}
    >
      {severityLabel(severity)}
      {score !== undefined ? ` · ${score}` : ""}
    </span>
  );
}
