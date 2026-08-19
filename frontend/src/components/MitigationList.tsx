import type { Mitigation } from "../types";

interface Props {
  mitigations: Mitigation[];
  onDelete: (id: number) => void;
}

export function MitigationList({ mitigations, onDelete }: Props) {
  if (mitigations.length === 0) {
    return <p className="muted">No mitigations yet. Add one below.</p>;
  }
  return (
    <ul className="mitigation-list">
      {mitigations.map((m) => (
        <li key={m.id}>
          <span>{m.description}</span>
          <span className="muted">effectiveness {m.effectiveness}/5</span>
          <button className="link-btn" onClick={() => onDelete(m.id)}>
            Remove
          </button>
        </li>
      ))}
    </ul>
  );
}
