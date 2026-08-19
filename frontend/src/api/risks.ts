import { request } from "./client";
import type {
  Category,
  Framework,
  MitigationRequest,
  Risk,
  RiskRequest,
  Status,
} from "../types";

export interface RiskFilters {
  category?: Category | "";
  status?: Status | "";
  sort?: "residualDesc" | "";
}

export function listRisks(filters: RiskFilters = {}): Promise<Risk[]> {
  const params = new URLSearchParams();
  if (filters.category) params.set("category", filters.category);
  if (filters.status) params.set("status", filters.status);
  if (filters.sort) params.set("sort", filters.sort);
  const qs = params.toString();
  return request<Risk[]>(`/risks${qs ? `?${qs}` : ""}`);
}

export const getRisk = (id: number) => request<Risk>(`/risks/${id}`);

export const createRisk = (body: RiskRequest) =>
  request<Risk>(`/risks`, { method: "POST", body });

export const updateRisk = (id: number, body: RiskRequest) =>
  request<Risk>(`/risks/${id}`, { method: "PUT", body });

export const deleteRisk = (id: number) =>
  request<void>(`/risks/${id}`, { method: "DELETE" });

export const addMitigation = (riskId: number, body: MitigationRequest) =>
  request<Risk>(`/risks/${riskId}/mitigations`, { method: "POST", body });

export const deleteMitigation = (id: number) =>
  request<void>(`/mitigations/${id}`, { method: "DELETE" });

export const listFrameworks = () => request<Framework[]>(`/frameworks`);
