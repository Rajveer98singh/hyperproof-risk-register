import { Link } from "react-router-dom";
import type { Risk } from "../types";
import { SeverityBadge } from "./SeverityBadge";
import { OverdueBadge } from "./OverdueBadge";
import { FrameworkChips } from "./FrameworkChips";

function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase();
}

/** Dashboard table. Residual severity is the visual anchor so the most severe surviving risks
 * stand out without reading every number. */
export function RiskTable({ risks }: { risks: Risk[] }) {
  if (risks.length === 0) {
    return <p className="muted">No risks match these filters.</p>;
  }
  return (
    <table className="risk-table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Category</th>
          <th>Owner</th>
          <th>Status</th>
          <th>Inherent</th>
          <th>Residual</th>
          <th>Mitigations</th>
          <th>Next review</th>
          <th>Frameworks</th>
        </tr>
      </thead>
      <tbody>
        {risks.map((r) => (
          <tr key={r.id}>
            <td>
              <Link to={`/risks/${r.id}`}>{r.title}</Link>
            </td>
            <td>{titleCase(r.category)}</td>
            <td>{r.owner ?? <span className="muted">—</span>}</td>
            <td>{titleCase(r.status)}</td>
            <td>
              <SeverityBadge severity={r.inherentSeverity} score={r.inherentScore} />
            </td>
            <td>
              <SeverityBadge severity={r.residualSeverity} score={r.residualScore} />
            </td>
            <td>{r.mitigationCount}</td>
            <td>
              <OverdueBadge nextReviewDate={r.nextReviewDate} overdue={r.overdue} />
            </td>
            <td>
              <FrameworkChips frameworks={r.frameworks} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
