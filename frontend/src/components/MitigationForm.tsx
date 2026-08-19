import { useState } from "react";
import type { MitigationRequest } from "../types";
import type { ApiError } from "../api/client";

interface Props {
  onAdd: (body: MitigationRequest) => Promise<void>;
}

const SCALE = [1, 2, 3, 4, 5];

export function MitigationForm({ onAdd }: Props) {
  const [description, setDescription] = useState("");
  const [effectiveness, setEffectiveness] = useState(3);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onAdd({ description, effectiveness });
      setDescription("");
      setEffectiveness(3);
    } catch (err) {
      setError((err as ApiError).detail ?? "Failed to add mitigation");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="mitigation-form" onSubmit={handleSubmit}>
      {error && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}
      <input
        placeholder="Describe the control…"
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <label>
        Effectiveness
        <select value={effectiveness} onChange={(e) => setEffectiveness(Number(e.target.value))}>
          {SCALE.map((n) => (
            <option key={n} value={n}>
              {n}
            </option>
          ))}
        </select>
      </label>
      <button type="submit" disabled={submitting || !description.trim()}>
        {submitting ? "Adding…" : "Add mitigation"}
      </button>
    </form>
  );
}
