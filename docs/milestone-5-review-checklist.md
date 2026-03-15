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

- [ ] **Approved**: Cross-dialect stabilization is complete; the library is ready for release or next-phase work.
- [ ] **Approved with follow-ups**: M5 is acceptable, but some non-blocking improvements should be addressed.
- [ ] **Changes required**: structural or consistency issues must be fixed before release.

Reference documents:

- `sql-parser-technical-design.md`
- `sql-parser-implementation-checklist.md` (§10)
- `milestone-5-cross-dialect-backlog.md`
- dialect coverage documentation

## 1. Diagnostics consistency

- [ ] Diagnostic structure (severity, line, column, message, offending token) is consistent across MySQL, SQLite, PostgreSQL, and Oracle for analogous errors.
- [ ] Any intentional dialect-specific diagnostic differences are documented.
- [ ] Cross-dialect tests verify diagnostic consistency for representative invalid SQL.
- [ ] `SyntaxDiagnostic` (and any extensions) are used consistently.

### Evidence to review

- `sqool-core` diagnostic types.
- Dialect parser implementations and error handling.
- Cross-dialect conformance tests.

### Review questions

1. Can consumers rely on a predictable diagnostic shape across dialects?
2. Are there any dialects with noticeably worse or inconsistent diagnostics?

## 2. AST coverage and consistency

- [ ] Shared constructs (SELECT, basic DDL, basic DML) produce consistent AST shapes across dialects where mapping exists.
- [ ] Dialect-specific constructs are modeled via extension nodes (e.g. `MySqlRawStatement`, `PostgresqlRawStatement`).
- [ ] AST coverage gaps are documented in the dialect coverage matrix.
- [ ] Golden tests exist for the expanded shared construct coverage.

### Evidence to review

- AST node definitions in `sqool-ast`.
- Dialect mapper implementations.
- Cross-dialect golden tests.

### Review questions

1. Is the normalized AST model coherent across dialects?
2. Are extension nodes used consistently for dialect-specific features?

## 3. Cross-dialect tests

- [ ] Cross-dialect tests cover the common subset across all four dialects (MySQL, SQLite, PostgreSQL, Oracle).
- [ ] Tests verify parse success and AST shape consistency for equivalent queries.
- [ ] Tests verify diagnostic structure for analogous syntax errors.
- [ ] Test structure is easy to extend when new dialects or constructs are added.

### Evidence to review

- `CrossDialectConformanceTest` and related tests.
- Test corpus and naming conventions.

### Review questions

1. Does the common subset have adequate cross-dialect coverage?
2. Are regressions likely to be caught?

## 4. Documentation and dialect coverage

- [ ] A dialect coverage matrix (or equivalent) exists and lists:
  - [ ] supported statement types and constructs per dialect,
  - [ ] normalized vs raw-statement mapping,
  - [ ] known limitations and unsupported features.
- [ ] `README.md` and technical design are up to date.
- [ ] Usage examples exist for parsing each dialect.
- [ ] Contributor guidance explains how to extend dialects and add tests.

### Evidence to review

- `README.md`
- `docs/dialect-coverage.md` or equivalent
- `CONTRIBUTING.md`
- Technical design document

### Review questions

1. Can a consumer quickly determine what is supported per dialect?
2. Can a contributor extend the library with confidence?

## 5. Benchmark reports

- [ ] Benchmarks run in CI and results are stored (e.g. as artifacts).
- [ ] Benchmark output format is documented.
- [ ] A comparison view or process exists for comparing MySQL, SQLite, PostgreSQL, and Oracle.
- [ ] Performance regressions can be detected by comparing results.

### Evidence to review

- CI configuration for benchmark runs.
- `docs/benchmarks.md`
- Artifact storage or report format.

### Review questions

1. Are benchmark results available and reproducible?
2. Is it clear how to interpret and compare results across dialects?

## 6. Naming, packaging, and API clarity

- [ ] Dialect module naming is consistent (`sqool-dialect-mysql`, `sqool-dialect-sqlite`, etc.).
- [ ] AST node naming and package structure are consistent across dialects.
- [ ] Shared parser and mapper helpers are documented or refactored for clarity.
- [ ] Patterns for adding new dialects or constructs are documented.

### Evidence to review

- Module structure and naming.
- Shared helper code in `sqool-core` and `sqool-ast`.
- Contributor documentation.

### Review questions

1. Is the codebase understandable for new contributors?
2. Are dialect extension patterns clear and consistent?

## 7. Exit criteria for review sign-off

Milestone 5 review is complete when:

- the milestone is classified as **Approved**, **Approved with follow-ups**, or **Changes required**,
- any blocking issues are documented,
- the team agrees the library is ready for release or the next phase of work (e.g. packaging and release readiness from the implementation checklist).
