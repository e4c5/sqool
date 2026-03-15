# Post-M5 Roadmap and Follow-ups

Milestones 0–5 are complete. This document records remaining follow-ups and optional work so the "next" steps are explicit. See also [SQL Parser Technical Design §19](sql-parser-technical-design.md#19-recommended-immediate-next-step) for recommended immediate next steps.

**For an actionable checklist with checkboxes**, use [Next Milestone Checklist](next-milestone-checklist.md).

## 1. Milestone 5 follow-ups (non-blocking)

**Done.** SQLite JOIN, PostgreSQL/Oracle DML, and NATURAL JOIN are all normalized; see [dialect-coverage.md](dialect-coverage.md) and [Next Milestone Checklist](next-milestone-checklist.md).

## 2. Optional Milestone 2 items

**Done.** Design note: [parser-abstractions.md](parser-abstractions.md). Baseline capture and naming: see [benchmarks.md](benchmarks.md) and [CONTRIBUTING.md](../CONTRIBUTING.md).

## 3. Optional Milestone 0 items

**Done.** All modules have source; documentation and dependency locking are in [CONTRIBUTING.md](../CONTRIBUTING.md) and root `build.gradle.kts`.

## 4. Milestone 1 – usage examples

Usage examples for parsing each dialect via the public API are in the [Dialect Coverage Matrix](dialect-coverage.md#usage-examples). For a quick link from the main README, see the "Usage" reference in [README.md](../README.md).

## 5. Future work (beyond follow-ups)

- **CTEs and subqueries** — Normalized AST for `WITH` (CTE) and subquery expressions across dialects.
- **ParseOptions / diagnostics** — `collectComments`, `ErrorTolerance`; extend `SyntaxDiagnostic` with `SourceSpan` and `expectedTokens` for richer tooling.
- **Release** — See [release-readiness.md](release-readiness.md) for the pre-publish checklist (module layout, versioning, CI, upstream grammar refresh).

---

*Last updated: Next-milestone pass; M5 follow-ups and optional M0/M2/§11–§12 complete.*
