import { CATEGORIES, STATUSES, type Category, type Status } from "../types";
import type { RiskFilters } from "../api/risks";

interface Props {
  filters: RiskFilters;
  onChange: (next: RiskFilters) => void;
}

function titleCase(s: string) {
  return s.charAt(0) + s.slice(1).toLowerCase();
}

export function Filters({ filters, onChange }: Props) {
  return (
    <div className="filters">
      <label>
        Category
        <select
          value={filters.category ?? ""}
          onChange={(e) => onChange({ ...filters, category: e.target.value as Category | "" })}
        >
          <option value="">All</option>
          {CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {titleCase(c)}
            </option>
          ))}
        </select>
      </label>

      <label>
        Status
        <select
          value={filters.status ?? ""}
          onChange={(e) => onChange({ ...filters, status: e.target.value as Status | "" })}
        >
          <option value="">All</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {titleCase(s)}
            </option>
          ))}
        </select>
      </label>

      <label className="checkbox">
        <input
          type="checkbox"
          checked={filters.sort === "residualDesc"}
          onChange={(e) => onChange({ ...filters, sort: e.target.checked ? "residualDesc" : "" })}
        />
        Sort by residual (high → low)
      </label>
    </div>
  );
}
