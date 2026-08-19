# CLAUDE.md

Guidance for Claude Code when working in this repo.

## Project
Hyperproof take-home: **Risk Register** — full-stack. Compliance teams track organizational
risks, score them (inherent + residual), attach mitigations (controls), and prioritize on a
severity-color-coded dashboard.

- `backend/` — Java 21, Spring Boot 3.x, Maven. REST API + JPA + Flyway. H2 (default) / Postgres (prod profile).
- `frontend/` — React 18 + TypeScript (strict) + Vite. Vitest + React Testing Library.

## Commands
```bash
# Backend
cd backend && mvn test              # all unit + slice + integration tests
cd backend && mvn spring-boot:run   # API on :8080 (H2, seeded), /h2-console dev-only
mvn spring-boot:run -Dspring-boot.run.profiles=prod   # Postgres (needs docker compose up)

# Frontend
cd frontend && npm install
cd frontend && npm test             # Vitest
cd frontend && npm run dev          # Vite dev server on :5173

# Postgres (optional, proves Flyway portability)
docker compose up -d
```

## Architecture rules (do not violate)
- **Scoring is pure & stateless** — `service/scoring/` has no DB/framework deps. Exhaustively
  unit-tested. This is the graded core; change it only with tests updated in lockstep.
- **Scores are DERIVED ON READ, never persisted.** Persist only raw inputs (likelihood, impact,
  effectiveness). `RiskMapper` computes scores via `ScoringService` on every response. Never add
  a stored `residualScore` column without a very good reason (stale-score data-integrity bug).
- **Layering**: `web` (thin controllers) → `service` (business rules) → `repository` → `domain`.
  Controllers never contain business logic or build error bodies.
- **DTOs at the boundary** — entities (`domain/`) are never serialized to JSON. Request/response
  are Java `record`s in `web/dto/`.
- **One error contract** — all 4xx/5xx go through `GlobalExceptionHandler` as RFC 9457
  `ProblemDetail` (+ `traceId`, `fieldErrors`). Never return ad-hoc error JSON from a controller.

## Domain facts
- **Inherent** = `likelihood × impact` (1–25).
- **Residual** = `clamp(round(inherent × (1 − (maxEffectiveness/5 × 0.8))), 1, inherent)`.
  Uses MAX mitigation effectiveness (strongest control sets the floor), capped at 80% reduction.
  Swappable via `ResidualScoringStrategy`.
- **Severity bands** (both scores): Low 1–5, Medium 6–12, High 13–19, Critical 20–25.
- **Business rule**: cannot set status `Closed` with zero mitigations → **409**.
- Validation ranges 1–5 enforced at BOTH Bean Validation (400) and DB CHECK constraints.

## Design patterns in use
Strategy (residual scoring), Specification (`RiskSpecifications` dynamic filters), Mapper
(`RiskMapper`), Repository (Spring Data). Keep patterns only where they earn their place.

## Security
- `owner` + `description` encrypted at rest via `EncryptedStringConverter` (AES-256-GCM, IV per
  value). Key from env `RISK_ENC_KEY` (`CryptoKeyProvider`); dev fallback only. **Never hardcode keys.**
- Filtering/sorting only on unencrypted enum columns (category/status) so encryption doesn't break queries.
- `SecurityHeadersFilter` sets nosniff / frame-deny / referrer / CSP. H2 console dev-profile only.
- No auth (single-org, per brief). No `dangerouslySetInnerHTML` on frontend.

## Conventions
- Conventional Commits. Frontend severity color logic (`lib/severity.ts`) mirrors backend bands —
  keep them in sync if bands change.
- Full plan: `/Users/rajveersingh/.claude/plans/hey-i-have-to-breezy-mccarthy.md`.
