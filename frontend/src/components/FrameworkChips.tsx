import type { Framework } from "../types";

/** Renders framework mappings as small chips, e.g. "NIST CSF · PR". */
export function FrameworkChips({ frameworks }: { frameworks: Framework[] }) {
  if (frameworks.length === 0) return <span className="muted">—</span>;
  return (
    <span className="chips">
      {frameworks.map((f) => (
        <span key={f.id} className="chip" title={f.name}>
          {f.framework.replace("_", " ")} · {f.code}
        </span>
      ))}
    </span>
  );
}
