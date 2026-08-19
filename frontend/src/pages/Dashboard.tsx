import { useState } from "react";
import { Link } from "react-router-dom";
import { useRisks } from "../hooks/useRisks";
import type { RiskFilters } from "../api/risks";
import { Filters } from "../components/Filters";
import { RiskTable } from "../components/RiskTable";
import { Spinner } from "../components/Spinner";
import { ErrorBanner } from "../components/ErrorBanner";

export function Dashboard() {
  const [filters, setFilters] = useState<RiskFilters>({ sort: "residualDesc" });
  const { risks, loading, error, reload } = useRisks(filters);

  return (
    <div>
      <div className="page-header">
        <h1>Risk Register</h1>
        <Link className="button" to="/risks/new">
          + New risk
        </Link>
      </div>

      <Filters filters={filters} onChange={setFilters} />

      {loading && <Spinner />}
      {error && <ErrorBanner message={error} onRetry={reload} />}
      {!loading && !error && <RiskTable risks={risks} />}
    </div>
  );
}
