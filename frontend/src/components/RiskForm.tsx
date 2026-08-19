import { useState } from "react";
import {
  CATEGORIES,
  STATUSES,
  type Category,
  type Framework,
  type Risk,
  type RiskRequest,
  type Status,
} from "../types";
import { inherentScore } from "../lib/scoring";
import { severityForScore } from "../lib/severity";
import { SeverityBadge } from "./SeverityBadge";
import type { ApiError } from "../api/client";

interface Props {
  initial?: Risk;
  frameworks: Framework[];
  submitLabel: string;
  onSubmit: (body: RiskRequest) => Promise<void>;
}

function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase();
}

const SCALE = [1, 2, 3, 4, 5];

export function RiskForm({ initial, frameworks, submitLabel, onSubmit }: Props) {
  const [title, setTitle] = useState(initial?.title ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [category, setCategory] = useState<Category>(initial?.category ?? "OPERATIONAL");
  const [owner, setOwner] = useState(initial?.owner ?? "");
  const [likelihood, setLikelihood] = useState(initial?.likelihood ?? 3);
  const [impact, setImpact] = useState(initial?.impact ?? 3);
  const [status, setStatus] = useState<Status>(initial?.status ?? "OPEN");
  const [nextReviewDate, setNextReviewDate] = useState(initial?.nextReviewDate ?? "");
  const [selectedFrameworks, setSelectedFrameworks] = useState<number[]>(
    initial?.frameworks.map((f) => f.id) ?? [],
  );

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const liveInherent = inherentScore(likelihood, impact);
  const liveSeverity = severityForScore(liveInherent);

  function toggleFramework(id: number) {
    setSelectedFrameworks((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFieldErrors({});
    setFormError(null);
    setSubmitting(true);
    try {
      await onSubmit({
        title,
        description: description || null,
        category,
        owner: owner || null,
        likelihood,
        impact,
        status,
        nextReviewDate: nextReviewDate || null,
        frameworkControlIds: selectedFrameworks,
      });
    } catch (err) {
      const apiErr = err as ApiError;
      if (apiErr.fieldErrors) setFieldErrors(apiErr.fieldErrors);
      setFormError(apiErr.detail ?? "Something went wrong");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="risk-form" onSubmit={handleSubmit}>
      {formError && (
        <div className="error-banner" role="alert">
          {formError}
        </div>
      )}

      <label>
        Title
        <input value={title} onChange={(e) => setTitle(e.target.value)} />
        {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
      </label>

      <label>
        Description
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} />
      </label>

      <div className="row">
        <label>
          Category
          <select value={category} onChange={(e) => setCategory(e.target.value as Category)}>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>
                {titleCase(c)}
              </option>
            ))}
          </select>
        </label>

        <label>
          Owner
          <input value={owner} onChange={(e) => setOwner(e.target.value)} />
        </label>

        <label>
          Status
          <select value={status} onChange={(e) => setStatus(e.target.value as Status)}>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {titleCase(s)}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="row">
        <label>
          Likelihood
          <select value={likelihood} onChange={(e) => setLikelihood(Number(e.target.value))}>
            {SCALE.map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </label>

        <label>
          Impact
          <select value={impact} onChange={(e) => setImpact(Number(e.target.value))}>
            {SCALE.map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </label>

        <div className="live-score">
          <span>Inherent score (live)</span>
          <SeverityBadge severity={liveSeverity} score={liveInherent} />
        </div>
      </div>

      <label>
        Next review date
        <input
          type="date"
          value={nextReviewDate ?? ""}
          onChange={(e) => setNextReviewDate(e.target.value)}
        />
      </label>

      <fieldset className="frameworks">
        <legend>Compliance frameworks</legend>
        {frameworks.map((f) => (
          <label key={f.id} className="checkbox">
            <input
              type="checkbox"
              checked={selectedFrameworks.includes(f.id)}
              onChange={() => toggleFramework(f.id)}
            />
            {f.framework.replace("_", " ")} · {f.code} — {f.name}
          </label>
        ))}
      </fieldset>

      <button type="submit" disabled={submitting}>
        {submitting ? "Saving…" : submitLabel}
      </button>
    </form>
  );
}
