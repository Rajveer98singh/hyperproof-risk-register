import { useEffect, useState } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import type { Framework, Risk, RiskRequest } from "../types";
import { createRisk, getRisk, listFrameworks, updateRisk } from "../api/risks";
import { RiskForm } from "../components/RiskForm";
import { Spinner } from "../components/Spinner";
import { ErrorBanner } from "../components/ErrorBanner";

/** Handles both create (`/risks/new`) and edit (`/risks/:id/edit`). */
export function RiskFormPage({ mode }: { mode: "new" | "edit" }) {
  const params = useParams();
  const navigate = useNavigate();
  const id = params.id ? Number(params.id) : undefined;

  const [frameworks, setFrameworks] = useState<Framework[]>([]);
  const [initial, setInitial] = useState<Risk | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loads: Promise<unknown>[] = [listFrameworks().then(setFrameworks)];
    if (mode === "edit" && id) {
      loads.push(getRisk(id).then(setInitial));
    }
    Promise.all(loads)
      .catch((e) => setError(e.message ?? "Failed to load form"))
      .finally(() => setLoading(false));
  }, [mode, id]);

  async function handleSubmit(body: RiskRequest) {
    const saved = mode === "edit" && id ? await updateRisk(id, body) : await createRisk(body);
    navigate(`/risks/${saved.id}`);
  }

  if (loading) return <Spinner />;
  if (error) return <ErrorBanner message={error} />;

  return (
    <div>
      <Link to={mode === "edit" && id ? `/risks/${id}` : "/"}>← Back</Link>
      <h1>{mode === "edit" ? "Edit risk" : "New risk"}</h1>
      <RiskForm
        initial={initial}
        frameworks={frameworks}
        submitLabel={mode === "edit" ? "Save changes" : "Create risk"}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
