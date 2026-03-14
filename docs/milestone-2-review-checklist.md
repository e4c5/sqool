# Milestone 2 Review Checklist – SQLite MVP

## Purpose

This checklist is for reviewing the **Milestone 2: SQLite MVP** implementation. It validates that SQLite is a first-class dialect, that MySQL patterns have been successfully generalized, and that duplication has been kept under control.

The review should focus on:

- extraction and reuse of shared parsing and mapping infrastructure,
- SQLite grammar vendoring and ANTLR integration,
- SQLite parser behavior and AST mapping,
- SQLite conformance and performance coverage,
- cross-dialect consistency between MySQL and SQLite.

The review should not spend much time on:

- PostgreSQL- or Oracle-specific plans (later milestones),
- full cross-dialect normalization beyond MySQL/SQLite overlap,
- long-term formatting or linting features.

## Recommended review outcome

At the end of the review, classify Milestone 2 as one of:

- [ ] **Approved**: SQLite MVP and generalization are strong enough to proceed to PostgreSQL.
- [ ] **Approved with follow-ups**: SQLite MVP is acceptable, though some non-blocking items should be addressed.
- [ ] **Changes required**: structural or correctness issues must be fixed before starting the PostgreSQL milestone.

Reference documents:

- `sql-parser-technical-design.md`
- `sql-parser-implementation-checklist.md` (§7)
- `milestone-1-mysql-backlog.md`
- `milestone-2-sqlite-backlog.md`

## 1. Dialect generalization and reuse

- [ ] Shared parsing and mapping infrastructure extracted from MySQL is:
  - [ ] clearly located (e.g., in `sqool-core`, `sqool-ast`, or focused shared packages),
  - [ ] used by both MySQL and SQLite,
  - [ ] free of dialect-specific assumptions.
- [ ] MySQL-specific code has not been blindly copy-pasted into SQLite.
- [ ] The shared components are named and documented clearly enough for later dialects.

### Evidence to review

- Shared parser helpers and utilities.
- Shared AST mapping helpers.
- MySQL and SQLite dialect implementations for similar flows.

### Review questions

1. Is there any obvious duplication that should be generalized now?
2. Will PostgreSQL and Oracle be able to reuse the same abstractions?

## 2. SQLite grammar vendoring and ANTLR integration

- [ ] SQLite grammar is vendored into `sqool-grammar-sqlite` with a clear `UPSTREAM.md`.
- [ ] `UPSTREAM.md` documents:
  - [ ] source repository and path,
  - [ ] commit hash,
  - [ ] local patches and why they are needed.
- [ ] ANTLR generation for SQLite runs as part of the normal Gradle build.
- [ ] Generated SQLite parser sources compile without manual changes.

### Evidence to review

- Grammar files in `sqool-grammar-sqlite`.
- ANTLR configuration for the SQLite module.
- SQLite parser smoke tests.

### Review questions

1. Are local grammar patches minimal and well-motivated?
2. Is the SQLite grammar layout consistent with the MySQL grammar module?

## 3. SQLite parser pipeline

- [ ] SQLite dialect implementation resides in `sqool-dialect-sqlite`.
- [ ] Parser setup follows the same SLL-fast / LL-fallback pattern as MySQL.
- [ ] Shared utilities are used for:
  - [ ] lexer/parser instantiation,
  - [ ] token stream configuration,
  - [ ] error listener and diagnostic wiring.
- [ ] Single-statement vs script entry points are defined where appropriate.
- [ ] Parser metrics for SQLite are captured via `ParseMetrics`.

### Evidence to review

- `sqool-dialect-sqlite` code.
- Any shared parser facade used by both dialects.
- Tests that cover both valid and invalid SQLite statements.

### Review questions

1. Is the SQLite parsing flow understandable and consistent with MySQL?
2. Are there any SQLite-specific quirks that should be better isolated?

## 4. SQLite AST mapping and dialect extensions

- [ ] The SQLite v1 subset is defined and documented.
- [ ] For shared SQL constructs, SQLite and MySQL map to the same normalized AST shapes.
- [ ] SQLite-specific constructs (e.g., conflict resolution clauses) use well-defined extension nodes.
- [ ] AST nodes remain immutable, compact, and free of ANTLR context references.
- [ ] Source spans are attached correctly when requested.
- [ ] Golden tests exist for representative SQLite statements.

### Evidence to review

- SQLite mapping code in `sqool-dialect-sqlite`.
- Shared AST helpers.
- Golden tests for SQLite ASTs.

### Review questions

1. Are there any places where SQLite mapping diverges unnecessarily from the normalized model?
2. Are dialect extensions for SQLite clearly separated from core AST types?

## 5. SQLite conformance and regression quality

- [ ] A SQLite-focused corpus exists and is:
  - [ ] versioned and reproducible,
  - [ ] based on real or realistic examples.
- [ ] Conformance tests:
  - [ ] assert success for valid queries,
  - [ ] assert expected diagnostics for malformed or unsupported queries,
  - [ ] validate AST structure where applicable.
- [ ] Regression tests are added for any SQLite issues found during the milestone.
- [ ] Test structure mirrors the MySQL layout where sensible.

### Evidence to review

- SQLite conformance and regression tests under `sqool-conformance` (or equivalent).
- Example SQL files for SQLite.

### Review questions

1. Can reviewers quickly understand which SQLite features are in or out of scope?
2. Are tests sufficient to prevent obvious regressions when expanding syntax support?

## 6. SQLite performance and benchmarks

- [ ] A SQLite benchmark corpus is defined across small, medium, large, and error-path categories.
- [ ] JMH benchmarks in `sqool-bench` exercise SQLite parsing with `sqool` (and JSqlParser where appropriate).
- [ ] Benchmarks report:
  - [ ] throughput,
  - [ ] latency,
  - [ ] allocation and GC behavior (where practical).
- [ ] Baseline SQLite metrics are recorded in a way that can be compared over time.
- [ ] Running SQLite benchmarks is documented for contributors.

### Evidence to review

- SQLite benchmark classes and configuration.
- Any captured benchmark results or reports.

### Review questions

1. Are SQLite benchmarks representative of real-world usage?
2. Is the benchmark harness usable during day-to-day development?

## 7. Cross-dialect consistency: MySQL vs SQLite

- [ ] There are explicit cross-dialect tests for shared constructs between MySQL and SQLite.
- [ ] For equivalent queries:
  - [ ] AST shapes are consistent,
  - [ ] diagnostics are comparable in structure and quality.
- [ ] Differences in behavior are documented and justified (e.g., dialect-specific syntax rules).

### Evidence to review

- Cross-dialect tests (MySQL vs SQLite).
- Any developer notes on dialect differences.

### Review questions

1. Would a consumer be surprised by major differences in behavior between MySQL and SQLite for the same logical query?
2. Are any observable inconsistencies better modeled as bugs or missing normalization?

## 8. Documentation and onboarding

- [ ] Documentation describes:
  - [ ] the current SQLite feature surface,
  - [ ] how to run SQLite tests,
  - [ ] how to run SQLite benchmarks.
- [ ] Known SQLite limitations are clearly described.
- [ ] Contributor guidance explains how to add or update SQLite-specific behavior.

### Evidence to review

- `README.md`
- `sql-parser-technical-design.md`
- `sql-parser-implementation-checklist.md`
- `milestone-2-sqlite-backlog.md`

### Review questions

1. Can a new contributor start working on SQLite code with confidence?
2. Are any planning documents now stale after the Milestone 2 implementation?

## 9. Exit criteria for review sign-off

Milestone 2 review is complete when:

- the milestone is classified as **Approved**, **Approved with follow-ups**, or **Changes required**,
- any blocking issues are clearly documented,
- the team agrees the repository is ready to begin the PostgreSQL grammar hardening milestone (or to address required SQLite/generalization changes first).

