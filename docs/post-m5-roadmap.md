# Post-M5 Roadmap and Follow-ups

Milestones 0–5 are complete. This document records remaining follow-ups and optional work so the "next" steps are explicit. See also [SQL Parser Technical Design §19](sql-parser-technical-design.md#19-recommended-immediate-next-step) for recommended immediate next steps.

## 1. Milestone 5 follow-ups (non-blocking)

These were noted in the [Milestone 5 Review Checklist](milestone-5-review-checklist.md) as acceptable follow-ups:

| Item | Description | Priority |
|------|-------------|----------|
| **SQLite JOIN normalization** | SQLite falls back to `SqliteRawStatement` for JOIN queries and complex WHERE expressions. Extend the v1 expression mapper so more SELECTs produce `SelectStatement` and `JoinTableReference`. | Medium |
| **PostgreSQL / Oracle DML normalization** | INSERT, UPDATE, DELETE are parsed but fall back to raw statements. Normalize to shared AST nodes where practical to match MySQL. | Medium |
| **NATURAL JOIN** | All dialects parse NATURAL JOIN but return raw statements. Normalize to a shared AST representation. | Low |

## 2. Optional Milestone 2 items

From [Milestone 2 SQLite Backlog](milestone-2-sqlite-backlog.md):

| Item | Description |
|------|-------------|
| **M2-1 design note** | Propose a short design note for `AntlrParserFacade` (or equivalent) and shared error/mapping helpers; refactor MySQL to use shared abstractions; document dialect-specific vs shared boundaries. |
| **M2-6 baseline results** | Capture baseline SQLite benchmark results in a reproducible form (e.g. CI artifact or committed report). |
| **M2-7 naming/API review** | Review naming, packaging, and shared helper APIs for clarity; document in CONTRIBUTING or a short doc. |

## 3. Optional Milestone 0 items

From [Milestone 0 Shortcomings](milestone-0-shortcomings.md):

| Item | Description |
|------|-------------|
| **Placeholder modules** | Add `src/main` and `src/test` placeholder directories and smoke tests for any inactive grammar/dialect modules that still lack them. |
| **Documentation** | Document: grammar generation workflow, benchmark reporting policy, dependency upgrade policy, contributor expectations (e.g. in README or CONTRIBUTING). |
| **Dependency locking** | Add dependency locking or an equivalent reproducibility mechanism for build-critical dependencies. |

## 4. Milestone 1 – usage examples

Usage examples for parsing each dialect via the public API are in the [Dialect Coverage Matrix](dialect-coverage.md#usage-examples). For a quick link from the main README, see the "Usage" reference in [README.md](../README.md).

## 5. Future work (beyond follow-ups)

- **CTEs and subqueries** — Normalized AST for `WITH` (CTE) and subquery expressions across dialects.
- **ParseOptions / diagnostics** — `collectComments`, `ErrorTolerance`; extend `SyntaxDiagnostic` with `SourceSpan` and `expectedTokens` for richer tooling.
- **Release** — Publish to Maven Central, API stability review, semantic versioning.

---

*Last updated: Post-M5 doc/backlog cleanup (Option A).*
