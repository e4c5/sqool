# Milestone 5 Review Checklist – Cross-Dialect Stabilization

## Purpose

This checklist is for reviewing the **Milestone 5: Cross-dialect stabilization** implementation. It validates that diagnostics, AST coverage, and documentation are consistent across all supported dialects, and that the project is ready for release or ongoing maintenance.

The review should focus on:

- consistency of diagnostic behavior across dialects,
- depth and consistency of AST mapping for shared constructs,
- quality of cross-dialect tests and documentation,
- benchmark report availability and usefulness,
- clarity of naming, packaging, and contributor guidance.

The review should not spend much time on:

- dialect-specific feature expansion beyond the agreed common subset,
- performance optimization (unless it is part of M5-5).

## Recommended review outcome

At the end of the review, classify Milestone 5 as one of:

- [x] **Approved with follow-ups**: M5 is acceptable, but some non-blocking improvements should be addressed.
- [ ] **Approved**: Cross-dialect stabilization is complete; the library is ready for release or next-phase work.
- [ ] **Changes required**: structural or consistency issues must be fixed before release.

**Classification: Approved with follow-ups.**

M5 deliverables are implemented. Key follow-ups (non-blocking):
- SQLite JOIN normalization: SQLite currently falls back to `SqliteRawStatement` for JOIN queries and complex WHERE expressions. This is documented and a known v1 scope limitation.
- INSERT/UPDATE/DELETE are not yet normalized for PostgreSQL and Oracle; they use raw statement fallback.
- NATURAL JOIN falls back to raw in all dialects.

Reference documents:

- `sql-parser-technical-design.md`
- `sql-parser-implementation-checklist.md` (§10)
- `milestone-5-cross-dialect-backlog.md`
- `docs/dialect-coverage.md`

## 1. Diagnostics consistency

- [x] Diagnostic structure (severity, line, column, message, offending token) is consistent across MySQL, SQLite, PostgreSQL, and Oracle for analogous errors.
- [x] Any intentional dialect-specific diagnostic differences are documented.
- [x] Cross-dialect tests verify diagnostic consistency for representative invalid SQL.
- [x] `SyntaxDiagnostic` (and any extensions) are used consistently.

**Evidence:** All four dialects use `AntlrSyntaxErrorListener` from `sqool-core`. See `docs/diagnostics.md` for the unified diagnostic contract. `CrossDialectConformanceTest.invalidSelectProducesStructuredDiagnosticsInAllFourDialects()` validates the structure across all four dialects.

### Review questions

1. Can consumers rely on a predictable diagnostic shape across dialects? **Yes** — severity, line, column, and offendingToken fields are always present and structurally consistent. Message text differs per dialect (ANTLR grammar-specific) but this is documented.
2. Are there any dialects with noticeably worse or inconsistent diagnostics? **No** — all four dialects use the same listener and field contract.

## 2. AST coverage and consistency

- [x] Shared constructs (SELECT, basic DDL, basic DML) produce consistent AST shapes across dialects where mapping exists.
- [x] Dialect-specific constructs are modeled via extension nodes (e.g. `MySqlRawStatement`, `PostgresqlRawStatement`).
- [x] AST coverage gaps are documented in the dialect coverage matrix.
- [x] Golden tests exist for the expanded shared construct coverage.

**Evidence:** JOIN queries now produce `SelectStatement + JoinTableReference` in MySQL, PostgreSQL, and Oracle. SQLite JOIN falls back to `SqliteRawStatement` (documented limitation). `docs/dialect-coverage.md` matrix lists normalized vs raw per construct.

### Review questions

1. Is the normalized AST model coherent across dialects? **Yes** — `SelectStatement`, `JoinTableReference`, `NamedTableReference`, and expression nodes are shared across all dialects.
2. Are extension nodes used consistently for dialect-specific features? **Yes** — each dialect has its own `{Dialect}RawStatement` and `{Dialect}StatementKind`.

## 3. Cross-dialect tests

- [x] Cross-dialect tests cover the common subset across all four dialects (MySQL, SQLite, PostgreSQL, Oracle).
- [x] Tests verify parse success and AST shape consistency for equivalent queries.
- [x] Tests verify diagnostic structure for analogous syntax errors.
- [x] Test structure is easy to extend when new dialects or constructs are added.

**Evidence:** `CrossDialectConformanceTest` includes: simple SELECT (all 4 dialects), SELECT with WHERE (parse success in all 4, SelectStatement in MySQL/PostgreSQL/Oracle), SELECT with JOIN (parse success in all 4, SelectStatement + JoinTableReference in MySQL/PostgreSQL/Oracle), INSERT and CREATE TABLE with raw statement verification, and diagnostic consistency test for all 4 dialects. Parameterized tests and helper methods make extension straightforward.

### Review questions

1. Does the common subset have adequate cross-dialect coverage? **Yes** — SELECT, INSERT, CREATE TABLE, and diagnostic failures are covered.
2. Are regressions likely to be caught? **Yes** — conformance tests and cross-dialect tests together cover the normalized subset.

## 4. Documentation and dialect coverage

- [x] A dialect coverage matrix (or equivalent) exists and lists:
  - [x] supported statement types and constructs per dialect,
  - [x] normalized vs raw-statement mapping,
  - [x] known limitations and unsupported features.
- [x] `README.md` and technical design are up to date.
- [x] Usage examples exist for parsing each dialect.
- [x] Contributor guidance explains how to extend dialects and add tests.

**Evidence:** `docs/dialect-coverage.md` contains the full matrix. `README.md` updated with M5 status and links. `CONTRIBUTING.md` includes module naming conventions, patterns for adding new dialects and new constructs, and shared helper guidance.

### Review questions

1. Can a consumer quickly determine what is supported per dialect? **Yes** — `docs/dialect-coverage.md` matrix is the single reference.
2. Can a contributor extend the library with confidence? **Yes** — `CONTRIBUTING.md` documents all patterns.

## 5. Benchmark reports

- [x] Benchmarks run in CI and results are stored (e.g. as artifacts).
- [x] Benchmark output format is documented.
- [x] A comparison view or process exists for comparing MySQL, SQLite, PostgreSQL, and Oracle.
- [x] Performance regressions can be detected by comparing results.

**Evidence:** `.github/workflows/ci.yml` has a `benchmark` job (runs on `main` only) that runs `./gradlew :sqool-bench:jmh` with JSON output and uploads as `jmh-results` artifact (90-day retention). JSON output format and a Python snippet for dialect comparison are documented in `docs/benchmarks.md`.

### Review questions

1. Are benchmark results available and reproducible? **Yes** — CI stores JSON artifacts on every main-branch push; can be reproduced locally with `./gradlew :sqool-bench:jmh -Pjmh.rf=json`.
2. Is it clear how to interpret and compare results across dialects? **Yes** — `docs/benchmarks.md` explains fields and provides a comparison script.

## 6. Naming, packaging, and API clarity

- [x] Dialect module naming is consistent (`sqool-dialect-mysql`, `sqool-dialect-sqlite`, etc.).
- [x] AST node naming and package structure are consistent across dialects.
- [x] Shared parser and mapper helpers are documented or refactored for clarity.
- [x] Patterns for adding new dialects or constructs are documented.

**Evidence:** Module naming follows `sqool-{type}-{dialect}` throughout. AST node naming follows `{Dialect}RawStatement` / `{Dialect}StatementKind`. Shared helpers (`AntlrSyntaxErrorListener`, `SourceSpans`, `ParseAttempt`, `ParseMetrics`) are documented in `CONTRIBUTING.md`. Adding new dialects and constructs is documented step-by-step in `CONTRIBUTING.md`.

### Review questions

1. Is the codebase understandable for new contributors? **Yes** — naming is consistent; `CONTRIBUTING.md` provides a clear onboarding path.
2. Are dialect extension patterns clear and consistent? **Yes** — Oracle/PostgreSQL JOIN implementation serves as a reference for future expansions.

## 7. Exit criteria for review sign-off

Milestone 5 review is complete when:

- the milestone is classified as **Approved**, **Approved with follow-ups**, or **Changes required**,
- any blocking issues are documented,
- the team agrees the library is ready for release or the next phase of work (e.g. packaging and release readiness from the implementation checklist).

**M5 is classified as: Approved with follow-ups.**

Non-blocking follow-ups:
1. SQLite JOIN normalization (`JoinTableReference`) not yet implemented; `SELECT` with JOINs falls back to `SqliteRawStatement`.
2. INSERT/UPDATE/DELETE normalization not yet implemented for PostgreSQL and Oracle.
3. NATURAL JOIN falls back to raw in all dialects.
