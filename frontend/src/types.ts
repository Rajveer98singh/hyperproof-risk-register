// Mirrors the backend DTOs. Enum values match the Java enum names exactly.

export type Category =
  | "OPERATIONAL"
  | "FINANCIAL"
  | "COMPLIANCE"
  | "SECURITY"
  | "STRATEGIC";

export type Status = "OPEN" | "MITIGATING" | "CLOSED";

export type Severity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export const CATEGORIES: Category[] = [
  "OPERATIONAL",
  "FINANCIAL",
  "COMPLIANCE",
  "SECURITY",
  "STRATEGIC",
];

export const STATUSES: Status[] = ["OPEN", "MITIGATING", "CLOSED"];

export interface Mitigation {
  id: number;
  description: string;
  effectiveness: number;
  createdAt: string;
}

export interface Framework {
  id: number;
  framework: string; // NIST_CSF | SOC2
  code: string;
  name: string;
}

export interface Risk {
  id: number;
  title: string;
  description: string | null;
  category: Category;
  owner: string | null;
  likelihood: number;
  impact: number;
  status: Status;
  inherentScore: number;
  inherentSeverity: Severity;
  residualScore: number;
  residualSeverity: Severity;
  mitigationCount: number;
  nextReviewDate: string | null;
  overdue: boolean;
  mitigations: Mitigation[];
  frameworks: Framework[];
  createdAt: string;
  updatedAt: string;
}

export interface RiskRequest {
  title: string;
  description?: string | null;
  category: Category;
  owner?: string | null;
  likelihood: number;
  impact: number;
  status: Status;
  nextReviewDate?: string | null;
  frameworkControlIds?: number[];
}

export interface MitigationRequest {
  description: string;
  effectiveness: number;
}
