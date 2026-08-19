import { Link, useNavigate, useParams } from "react-router-dom";
import type { Mitigation, MitigationRequest, Risk } from "../types";
import { useRisk } from "../hooks/useRisk";
import { addMitigation, deleteMitigation, deleteRisk } from "../api/risks";
import { SeverityBadge } from "../components/SeverityBadge";
import { MitigationList } from "../components/MitigationList";
import { MitigationForm } from "../components/MitigationForm";
import { FrameworkChips } from "../components/FrameworkChips";
import { OverdueBadge } from "../components/OverdueBadge";
import { Spinner } from "../components/Spinner";
import { ErrorBanner } from "../components/ErrorBanner";

function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase();
}

export function RiskDetail() {
  const { id } = useParams();
  const riskId = Number(id);
  const navigate = useNavigate();
  const { risk, setRisk, loading, error, reload } = useRisk(riskId);

  if (loading) return <Spinner />;
  if (error) return <ErrorBanner message={error} onRetry={reload} />;
  if (!risk) return null;

  async function handleAdd(body: MitigationRequest) {
    const current = risk as Risk;
    // Optimistic: show the new mitigation immediately with a temporary id.
    const optimistic: Mitigation = {
      id: -Date.now(),
      description: body.description,
      effectiveness: body.effectiveness,
      createdAt: new Date().toISOString(),
    };
    setRisk({
      ...current,
      mitigations: [...current.mitigations, optimistic],
      mitigationCount: current.mitigationCount + 1,
    });
    try {
      // Server returns the parent risk with the recomputed residual score.
      const updated = await addMitigation(riskId, body);
      setRisk(updated);
    } catch (e) {
      reload(); // rollback to server truth
      throw e;
    }
  }

  async function handleDeleteMitigation(mitigationId: number) {
    await deleteMitigation(mitigationId);
    reload();
  }

  async function handleDeleteRisk() {
    if (!confirm("Delete this risk and all its mitigations?")) return;
    await deleteRisk(riskId);
    navigate("/");
  }

  return (
    <div>
      <Link to="/">← Back to dashboard</Link>
      <div className="page-header">
        <h1>{risk.title}</h1>
        <div className="actions">
          <Link className="button" to={`/risks/${risk.id}/edit`}>
            Edit
          </Link>
          <button className="button danger" onClick={handleDeleteRisk}>
            Delete
          </button>
        </div>
      </div>

      <div className="detail-grid">
        <div>
          <span className="label">Category</span>
          {titleCase(risk.category)}
        </div>
        <div>
          <span className="label">Owner</span>
          {risk.owner ?? "—"}
        </div>
        <div>
          <span className="label">Status</span>
          {titleCase(risk.status)}
        </div>
        <div>
          <span className="label">Likelihood × Impact</span>
          {risk.likelihood} × {risk.impact}
        </div>
        <div>
          <span className="label">Inherent</span>
          <SeverityBadge severity={risk.inherentSeverity} score={risk.inherentScore} />
        </div>
        <div>
          <span className="label">Residual</span>
          <SeverityBadge severity={risk.residualSeverity} score={risk.residualScore} />
        </div>
        <div>
          <span className="label">Next review</span>
          <OverdueBadge nextReviewDate={risk.nextReviewDate} overdue={risk.overdue} />
        </div>
        <div>
          <span className="label">Frameworks</span>
          <FrameworkChips frameworks={risk.frameworks} />
        </div>
      </div>

      {risk.description && <p className="description">{risk.description}</p>}

      <h2>Mitigations</h2>
      <MitigationList mitigations={risk.mitigations} onDelete={handleDeleteMitigation} />
      <MitigationForm onAdd={handleAdd} />
    </div>
  );
}
