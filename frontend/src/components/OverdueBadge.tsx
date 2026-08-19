interface Props {
  nextReviewDate: string | null;
  overdue: boolean;
}

/** Shows the next review date, flagged red when the backend reports it overdue. */
export function OverdueBadge({ nextReviewDate, overdue }: Props) {
  if (!nextReviewDate) return <span className="muted">—</span>;
  return (
    <span className={overdue ? "badge overdue" : "muted"} title="Next review date">
      {overdue ? "⚠ Overdue " : ""}
      {nextReviewDate}
    </span>
  );
}
