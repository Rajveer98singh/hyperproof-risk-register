import { useCallback, useEffect, useState } from "react";
import type { Risk } from "../types";
import { getRisk } from "../api/risks";

/** Loads a single risk. `setRisk` lets callers apply a server response (e.g. after adding a
 * mitigation) so the residual score updates without a separate refetch. */
export function useRisk(id: number) {
  const [risk, setRisk] = useState<Risk | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    getRisk(id)
      .then(setRisk)
      .catch((e) => setError(e.message ?? "Failed to load risk"))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => load(), [load]);

  return { risk, setRisk, loading, error, reload: load };
}
