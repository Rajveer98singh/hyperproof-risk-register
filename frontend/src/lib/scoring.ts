// Client-side inherent score for live form feedback before any network round-trip.
// The backend remains the source of truth for the residual score.
export function inherentScore(likelihood: number, impact: number): number {
  return likelihood * impact;
}
