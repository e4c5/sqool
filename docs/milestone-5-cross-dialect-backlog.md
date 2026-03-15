# Milestone 5 Backlog – Cross-Dialect Stabilization

## Status

- Draft: v0.1
- Scope: SQL Parser Technical Design – Milestone 5 (Cross-dialect stabilization)
- Related docs:
  - `sql-parser-technical-design.md` (§17 Milestones)
  - `sql-parser-implementation-checklist.md` (§10 Cross-dialect normalization)
  - `milestone-1-mysql-backlog.md`
  - `milestone-2-sqlite-backlog.md`
  - `milestone-3-postgresql-backlog.md`
  - `milestone-4-oracle-backlog.md`

## 1. Purpose

This document expands the **Milestone 5: Cross-dialect stabilization** line item into a concrete implementation backlog.

Milestone 5 should leave the repository in a state where:

- Diagnostics behavior is consistent across all supported dialects.
- AST coverage for shared constructs is expanded where practical.
- Cross-dialect golden tests validate the common subset.
- Documentation clearly describes supported constructs per dialect.
- Benchmark reports are published in CI or a reproducible form.

Consumers should be able to use one API and one core AST model across MySQL, SQLite, PostgreSQL, and Oracle with predictable, consistent behavior.

## 2. Constraints and scope

Milestone 5 builds on:

- All four dialects (MySQL, SQLite, PostgreSQL, Oracle) implemented and stable from Milestones 1–4.
- The existing cross-dialect tests (MySQL vs SQLite, and any extensions).
- The technical design goal: consistent AST and diagnostic behavior across dialects.

Cross-dialect work must:

- Preserve backward compatibility for each dialect's public behavior.
- Avoid breaking changes to the normalized AST or `ParseResult` API.
- Prioritize consistency over feature expansion.

## 3. Milestone 5 definition of done

Milestone 5 is complete when all of the following are true:

1. Diagnostics behavior is unified or explicitly documented per dialect where it differs.
2. AST coverage for shared constructs (SELECT, DDL, DML) is expanded where practical across dialects.
3. Cross-dialect golden tests exist for the common subset (MySQL, SQLite, PostgreSQL, Oracle where applicable).
4. Documentation describes supported constructs, limitations, and dialect differences clearly.
5. Benchmark reports are published in CI or a reproducible form.
6. Naming, packaging, and shared helper APIs have been reviewed for clarity.

## 4. Work breakdown structure

### Epic M5-1: Diagnostics unification and documentation

**Objective**

Ensure diagnostics are consistent across dialects for analogous errors, or document intentional differences.

**Tasks**

- [x] Audit diagnostic structure (severity, line, column, message, offending token) across all dialects.
- [x] Identify and fix or document any dialect-specific diagnostic quirks.
- [x] Add or extend cross-dialect tests that verify analogous syntax errors produce consistent diagnostic structure.
- [x] Consider extending `SyntaxDiagnostic` with `SourceSpan` and `expectedTokens` if needed for consistency.
- [x] Document diagnostic behavior in the technical design or a dedicated doc.

**Deliverables**

- Unified or documented diagnostic behavior.
- Cross-dialect diagnostic consistency tests.

**Dependencies**

- Milestones 1–4 complete.

**Acceptance criteria**

- Consumers can rely on a consistent diagnostic shape across dialects for analogous errors.
- Any intentional differences are documented.

### Epic M5-2: AST coverage expansion for shared constructs

**Objective**

Expand normalized AST mapping for shared constructs across dialects where gaps exist.

**Tasks**

- [x] Identify shared constructs that are normalized in some dialects but raw in others (e.g. joins, CTEs, DML).
- [x] Prioritize expansion based on:
  - [x] consumer value,
  - [x] grammar/mapper complexity,
  - [x] consistency with existing patterns.
- [/] Implement normalized mapping for the highest-priority shared constructs in each dialect.
- [x] Add golden tests for the expanded coverage.
- [x] Document the expanded v1/v2 subset per dialect.

**Deliverables**

- Expanded AST mapping for shared constructs.
- Updated dialect coverage documentation.

**Dependencies**

- M5-1 (diagnostics baseline).

**Acceptance criteria**

- Shared constructs produce consistent AST shapes across dialects where mapping exists.
- Dialect-specific constructs remain in extension nodes.

### Epic M5-3: Cross-dialect golden tests

**Objective**

Establish cross-dialect golden tests for the common SQL subset across all four dialects.

**Tasks**

- [x] Define the common subset for cross-dialect testing (SELECT, basic DDL, basic DML).
- [x] Add cross-dialect tests that:
  - [x] parse identical or equivalent queries in MySQL, SQLite, PostgreSQL, and Oracle,
  - [x] verify parse success and AST shape consistency where applicable,
  - [x] cover the shared expression and clause constructs.
- [x] Include PostgreSQL and Oracle in `CrossDialectConformanceTest` (or equivalent) where syntax overlaps.
- [x] Add regression tests for any cross-dialect bugs discovered.

**Deliverables**

- Cross-dialect golden tests for the common subset.
- Test structure that is easy to extend when new dialects or constructs are added.

**Dependencies**

- M5-2 (AST coverage expansion, or at least baseline).

**Acceptance criteria**

- The common subset is validated across all supported dialects.
- Cross-dialect regressions are caught by tests.

### Epic M5-4: Documentation and dialect coverage matrix

**Objective**

Produce clear documentation of supported constructs, limitations, and dialect differences.

**Tasks**

- [x] Create or update a **dialect coverage matrix** (e.g. in `README.md` or `docs/dialect-coverage.md`) that:
  - [x] lists supported statement types and constructs per dialect,
  - [x] indicates normalized vs raw-statement mapping,
  - [x] documents known limitations and unsupported features.
- [x] Update `sql-parser-technical-design.md` with any changes from M5.
- [x] Ensure `CONTRIBUTING.md` and dialect-specific docs are up to date.
- [x] Add or update usage examples for parsing each dialect.

**Deliverables**

- Dialect coverage matrix or equivalent documentation.
- Updated technical design and contributor guidance.

**Dependencies**

- M5-2 and M5-3 (to accurately describe coverage).

**Acceptance criteria**

- Consumers can quickly determine what is supported per dialect.
- Contributors know how to extend coverage and add tests.

### Epic M5-5: Benchmark report publication

**Objective**

Publish benchmark reports in CI or a reproducible form so performance can be tracked over time.

**Tasks**

- [x] Configure CI to run benchmarks and store results (e.g. as artifacts or in a report).
- [x] Ensure benchmark output format (JSON, CSV, or human-readable) is documented.
- [x] Add a comparison view or script that compares MySQL, SQLite, PostgreSQL, and Oracle baselines.
- [x] Document how to interpret benchmark results and compare across dialects.

**Deliverables**

- CI benchmark runs and artifact storage.
- Documented benchmark report format and comparison process.

**Dependencies**

- All four dialects have benchmarks (from M1–M4).

**Acceptance criteria**

- Benchmark results are available for each CI run or on demand.
- Performance regressions can be detected by comparing results.

### Epic M5-6: Naming, packaging, and API review

**Objective**

Review naming, packaging, and shared helper APIs for clarity and consistency.

**Tasks**

- [x] Review dialect module naming (`sqool-dialect-mysql`, etc.) for consistency.
- [x] Review AST node naming and package structure across dialects.
- [x] Identify any shared parser or mapper helpers that could be generalized.
- [x] Propose and implement refactors that improve clarity without breaking the public API.
- [x] Document the recommended patterns for adding new dialects or constructs.

**Deliverables**

- Naming and packaging review notes.
- Refactored or documented shared helper patterns.
- Contributor guidance for dialect extension.

**Dependencies**

- All previous M5 epics (to avoid refactoring mid-implementation).

**Acceptance criteria**

- The codebase is understandable and consistent for contributors.
- Patterns for dialect extension are clear and documented.

## 5. Suggested execution order

The preferred implementation sequence inside Milestone 5 is:

1. M5-1 Diagnostics unification and documentation
2. M5-2 AST coverage expansion for shared constructs
3. M5-3 Cross-dialect golden tests
4. M5-4 Documentation and dialect coverage matrix
5. M5-5 Benchmark report publication
6. M5-6 Naming, packaging, and API review

This sequence ensures diagnostics are stable before expanding AST coverage, and documentation reflects the final state before the naming/packaging review.
