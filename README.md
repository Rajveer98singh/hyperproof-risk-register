# Risk Register

A small full-stack Risk Register: a Spring Boot (Java 21) REST API and a React + TypeScript
frontend. Compliance teams create organizational risks, score them (inherent + residual), attach
mitigations (controls), and prioritize on a severity-color-coded dashboard.

- **Backend** — Java 21, Spring Boot 3.3, JPA/Hibernate, Flyway. H2 (default) / PostgreSQL.
- **Frontend** — React 18, TypeScript (strict), Vite, Vitest.

---

## Quick start (< 5 minutes)

Two terminals. No database to install — the backend runs on an in-memory H2 database out of the box.

**1. Backend** (http://localhost:8080)
```bash
cd backend
mvn spring-boot:run
```
Flyway builds the schema and a small set of sample risks is seeded automatically (dev profile).

**2. Frontend** (http://localhost:5173)
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173. The Vite dev server proxies `/api` to the backend, so there is nothing
else to configure.

### Run the tests (single command each)
```bash
cd backend  && mvn test     # 48 tests: scoring, repository, service, retry, full API integration
cd frontend && npm test      # 16 tests: severity/scoring libs, badge, form live-score, api client
```

---

## Scoring

### Inherent risk
`inherent = likelihood × impact` → range **1–25**.

### Residual risk (chosen formula)
A risk is reduced by the **strength of the strongest control protecting it**:

```
effMax          = max(effectiveness of the risk's mitigations)   // 0 if none
reductionFactor = (effMax / 5) × 0.8                              // eff 5 → 0.80, eff 1 → 0.16
residual        = clamp( round( inherent × (1 − reductionFactor) ), 1, inherent )
```

**Why `max` effectiveness (and not a product or an average)?**
- A **multiplicative** decay `∏(1 − effᵢ/5)` lets several weak controls drive the residual toward
  zero — it *overstates* protection, which is exactly the kind of artifact an auditor would
  challenge.
- An **average** *understates* a strong control when it sits next to weaker ones.
- **Max** is the honest, explainable middle: your best applicable control sets the floor of
  protection. The trade-off — it ignores defense-in-depth from stacking controls — is noted under
  "With more time" as a diminishing-returns refinement.

**Why the 0.8 cap?** No control is perfect, so residual risk must always remain. Capping the
reduction at 80% means even a maximally effective single control can't zero out a Critical risk.

The formula satisfies every sanity check in the brief:
| Scenario | Result |
| --- | --- |
| Zero mitigations | residual = inherent |
| One highly effective mitigation (eff 5) on a 25 | residual = 5 (Critical → Low) |
| Any mitigation | residual never below 1, never above inherent |

The logic lives in a pure, dependency-free `ScoringService` / `MaxEffectivenessStrategy`
(Strategy pattern) and is the most heavily unit-tested part of the codebase.

### Severity bands (applied to both scores)
`Low 1–5 · Medium 6–12 · High 13–19 · Critical 20–25`.

### Data-integrity note
Scores are **computed on every read and never persisted**. Only raw inputs (likelihood, impact,
mitigation effectiveness) are stored, so a stored score can never drift out of sync with its inputs.

---

## Business rule: closing a risk with no mitigations

**A risk cannot be set to `Closed` while it has zero mitigations → HTTP 409.**

Reasoning: in a compliance/audit product, marking a risk *resolved* with no evidence that any
control was ever applied is precisely the gap an auditor looks for. Requiring at least one
mitigation before closing keeps the register defensible. Once a mitigation exists, closing is
allowed regardless of the residual score — a team may knowingly accept residual risk, and that is a
valid, logged decision.

---

## API

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/risks` | Create. 201 + risk. |
| GET | `/api/risks?category=&status=&sort=residualDesc` | List with optional filters + residual sort. |
| GET | `/api/risks/{id}` | Get one. |
| PUT | `/api/risks/{id}` | Update. |
| DELETE | `/api/risks/{id}` | 204. |
| POST | `/api/risks/{riskId}/mitigations` | Add mitigation; returns the parent risk (with updated residual). |
| PUT | `/api/mitigations/{id}` | Update mitigation; returns parent risk. |
| DELETE | `/api/mitigations/{id}` | 204. |
| GET | `/api/frameworks` | Compliance-framework catalog (NIST CSF + SOC 2). |

### Validation & errors
`likelihood`, `impact`, `effectiveness` must be integers 1–5, enforced at **two layers**: Bean
Validation (→ 400 with field messages) and database `CHECK` constraints (defense in depth). Every
error is returned as an RFC 9457 `ProblemDetail`, with a `traceId` for correlation and a
`fieldErrors` map on validation failures:

```json
{ "title": "Validation Failed", "status": 400, "detail": "Validation failed",
  "traceId": "…", "fieldErrors": { "likelihood": "must be less than or equal to 5" } }
```

Status mapping: `400` validation / malformed JSON / bad enum · `404` not found · `409` business-rule
violation and concurrent-modification · `500` unexpected (logged, never leaked).

### Quick smoke test
```bash
# create → add mitigation → observe residual drop
ID=$(curl -s -X POST localhost:8080/api/risks -H 'Content-Type: application/json' \
  -d '{"title":"Breach","category":"SECURITY","likelihood":5,"impact":5,"status":"OPEN"}' | jq .id)
curl -s -X POST localhost:8080/api/risks/$ID/mitigations -H 'Content-Type: application/json' \
  -d '{"description":"WAF + patch","effectiveness":5}' | jq '{inherentScore, residualScore, residualSeverity}'
# → inherentScore 25, residualScore 5, residualSeverity "LOW"
```

---

## Frontend

- **Dashboard** — table of risks with title, category, owner, status, inherent + residual scores,
  mitigation count, next-review, and framework chips. Severity is shown as a colored pill **with a
  text label** (readable for color-blind users), so the most severe surviving risks stand out
  without reading the numbers. Filter by category/status and sort by residual.
- **Create / edit form** — shows the **inherent score live** as likelihood/impact change (computed
  client-side); server validation errors surface inline.
- **Risk detail** — full risk with its mitigations and an add-mitigation form; the residual score
  updates immediately after a mitigation is added (optimistic update, reconciled with the server
  response).

---

## Architecture & design decisions

- **Layering**: thin controllers → services (own the business rules) → repositories → domain.
  Entities are never serialized; Java `record` DTOs form the API boundary, mapped inside the
  transaction so scores compute and lazy associations load with `open-in-view` disabled.
- **Design patterns**: Strategy (`ResidualScoringStrategy`), Specification (`RiskSpecifications`
  for composable optional filters), Mapper (`RiskMapper`), Repository (Spring Data).
- **Concurrency**: optimistic locking via a `@Version` column. Updates are `@Retryable` on a version
  conflict (3 attempts, exponential backoff), each attempt re-reading the latest row; if all attempts
  lose the race the client gets a clean 409.
- **Migrations**: Flyway owns the schema (`ddl-auto: validate`), so the same `V1`/`V2` migrations run
  unchanged on H2 and PostgreSQL.

### Security & data protection (sensible, not over-engineered)
- **Encryption at rest**: `owner` and `description` (potential PII / sensitive detail) are encrypted
  with **AES-256-GCM** via a JPA `AttributeConverter` (fresh random IV per value, authenticated so
  tampering is detected). The key comes from the `RISK_ENC_KEY` environment variable; a documented
  dev fallback lets the app run with zero setup. Filtering/sorting is only ever done on the
  unencrypted enum columns, so encryption doesn't break queries.
- **Security headers** (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, a strict
  `Content-Security-Policy`) via a lightweight servlet filter — no need to pull in full Spring
  Security / auth, which the brief excludes.
- **Injection-safe** JPA parameterized queries only; **CORS** locked to the dev origin; the **H2
  console is enabled in the dev profile only**; error responses never leak stack traces.

---

## Using real PostgreSQL (optional)

H2 is the zero-setup default. To prove the migrations are portable, a Postgres path is included:
```bash
cd backend
docker compose up -d                                   # starts postgres:16
mvn spring-boot:run -Dspring-boot.run.profiles=prod    # same Flyway migrations run on Postgres
```

---

## Assumptions, trade-offs, and intentionally skipped

- **H2 in-memory by default** — data resets on restart. Chosen for a < 5-minute setup; the schema
  and code are Postgres-ready (see above), so switching is a profile change.
- **No auth / multi-tenancy** — per the brief; a single implicit organization is assumed.
- **Residual sort is computed in the service** (residual isn't a stored column). Fine for a register
  of hundreds of risks; a materialized column would be the move at much larger scale.
- **Mitigation combination uses max effectiveness** — see the reasoning above; a defense-in-depth
  model is a deliberate future refinement.
- **Skipped edge cases**: pagination, bulk operations, soft-delete/audit history, request rate
  limiting, and a secrets manager for the encryption key — all called out below.

## With more time
- PostgreSQL + Flyway as the default with a proper KDF (PBKDF2/Argon2) + salt for the encryption key,
  sourced from a secrets manager.
- Pagination and server-side sorting for large registers (materialized residual score).
- A richer residual model (diminishing-returns blend of controls) behind the existing Strategy seam.
- Audit log of score/status changes; per-field access control.
- Frontend: React Query for caching/optimistic built-ins; end-to-end (Playwright) tests.
