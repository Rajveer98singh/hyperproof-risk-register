import { useCallback, useEffect, useState } from "react";
import type { Risk } from "../types";
import { listRisks, type RiskFilters } from "../api/risks";

/** Loads the risk list for the given filters, exposing loading/error state and a reload trigger. */
export function useRisks(filters: RiskFilters) {
  const [risks, setRisks] = useState<Risk[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const key = JSON.stringify(filters);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    listRisks(filters)
      .then(setRisks)
      .catch((e) => setError(e.message ?? "Failed to load risks"))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  useEffect(() => load(), [load]);

  return { risks, loading, error, reload: load };
}
