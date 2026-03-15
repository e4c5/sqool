# Milestone 4 Review Checklist – Oracle SQL MVP

## Purpose

This checklist is for reviewing the **Milestone 4: Oracle SQL MVP** implementation. It validates that the Oracle grammar has been safely vendored and subset to SQL-first support, that the Oracle parser is stable for a defined v1 subset, and that the project is ready to proceed to cross-dialect stabilization.

The review should focus on:

- clarity and maintainability of the Oracle grammar subset,
- stability and predictability of the Oracle parser pipeline,
- correctness and clarity of AST mapping for the v1 subset,
- depth of Oracle conformance and regression coverage,
- Oracle performance characteristics and documented risks.

The review should not spend much time on:

- full Oracle or PL/SQL feature completeness (beyond the agreed v1 subset),
- cross-dialect normalization that is targeted by Milestone 5.

## Recommended review outcome

At the end of the review, classify Milestone 4 as one of:

- [x] **Approved**: Oracle grammar and parser are stable enough to proceed to cross-dialect stabilization.
- [ ] **Approved with follow-ups**: M4 is acceptable, but some non-blocking improvements should be addressed.
- [ ] **Changes required**: structural or correctness issues must be fixed before starting Milestone 5.

Reference documents:

- `sql-parser-technical-design.md` (Oracle scope, §12.3)
- `sql-parser-implementation-checklist.md` (§9)
- `milestone-4-oracle-backlog.md`
- any Oracle-specific notes (`UPSTREAM.md`, risk and scope notes)

## 1. Oracle grammar vendoring and subset quality

- [x] Oracle grammar is vendored into `sqool-grammar-oracle`.
- [x] `UPSTREAM.md` (or equivalent) documents:
  - [x] upstream repository and path,
  - [x] chosen commit hash,
  - [x] known upstream issues,
  - [x] local goals (SQL-first subset).
- [x] Grammar is subset or gated to exclude broad PL/SQL (anonymous blocks, procedural constructs).
- [x] Grammar source layout (lexer/parser `.g4` files) is clear and consistent with other grammar modules.
- [x] ANTLR generation for Oracle is integrated into the normal Gradle build.
- [x] Generated sources compile cleanly without manual edits.

### Evidence to review

- `sqool-grammar-oracle` directory contents.
- ANTLR configuration in `build.gradle.kts` for the Oracle module.
- `UPSTREAM.md` for Oracle.

### Review questions

1. Is the SQL-first subset boundary clear enough to avoid PL/SQL sprawl?
2. Are any local patches or subsetting decisions well documented?

## 2. Oracle parser pipeline

- [x] Oracle dialect implementation resides in `sqool-dialect-oracle`.
- [x] Parser setup follows the established pattern:
  - [x] SLL-fast path with bail-fast error strategy,
  - [x] LL-fallback path with structured diagnostics.
- [x] Shared parser utilities are used wherever reasonable (lexer/parser setup, error listeners, metrics).
- [x] Single-statement vs script entry points are defined where applicable.
- [x] `SqlDialect` and `ParseOptions` support Oracle cleanly.
- [x] `ParseMetrics` capture Oracle behavior for benchmarking and debugging.

### Evidence to review

- `sqool-dialect-oracle` code.
- Tests that exercise both successful and failing Oracle parses.

### Review questions

1. Is the Oracle parsing flow understandable and maintainable?
2. Are error messages and diagnostics up to the standard set by MySQL/SQLite/PostgreSQL?

## 3. Oracle v1 subset and AST mapping

- [x] The Oracle SQL v1 subset is explicitly defined (in docs or code comments).
- [x] For the v1 subset:
  - [x] AST mapping covers basic DDL, DML, and `SELECT`-style queries,
  - [x] common constructs reuse the normalized AST model and shared helpers,
  - [x] Oracle-specific constructs are represented via extension nodes.
- [x] AST nodes remain:
  - [x] immutable,
  - [x] compact,
  - [x] free of ANTLR context references.
- [x] Source spans are attached properly when `ParseOptions` request them.
- [x] Golden tests exist for representative Oracle statements in the v1 subset.

### Evidence to review

- Oracle mapping code in `sqool-dialect-oracle`.
- AST node definitions in `sqool-ast`.
- Golden test assets.

### Review questions

1. Is the v1 subset well-scoped and realistic?
2. Are Oracle-specific features modeled in a way that will scale to later expansion?

## 4. Oracle conformance and regression quality

- [x] An Oracle SQL corpus exists and is:
  - [x] sourced from vendor docs and realistic examples,
  - [x] organized clearly with version control.
- [x] Conformance tests:
  - [x] cover core v1 constructs,
  - [x] verify both successful parses and expected failures,
  - [x] validate AST structures where appropriate.
- [x] Regression tests cover grammar or mapping bugs found during M4.
- [x] Test structure is consistent with MySQL, SQLite, and PostgreSQL suites.

### Evidence to review

- Oracle test suites and SQL files under `sqool-conformance`.
- Regression test files and naming conventions.

### Review questions

1. Is it easy to see what Oracle features are supported today?
2. Are tests likely to catch future regressions in the v1 subset?

## 5. Oracle performance and benchmarks

- [x] An Oracle benchmark corpus is defined for:
  - [x] small,
  - [x] medium,
  - [x] error-path statements.
- [x] JMH benchmarks in `sqool-bench` exercise Oracle parsing using:
  - [x] the `sqool` Oracle dialect,
  - [x] JSqlParser where the same statements are supported.
- [x] Benchmarks report throughput and latency.
- [x] How to run Oracle benchmarks and capture baseline is documented.

### Evidence to review

- Oracle benchmark classes and Gradle configuration.
- `docs/benchmarks.md` or equivalent.

### Review questions

1. Are any Oracle-specific performance pathologies obvious from the data?
2. Is there a clear plan for addressing performance issues, if present?

## 6. Risk and scope documentation

- [x] Oracle-specific risks from the technical design have been reviewed against actual M4 work.
- [x] A short **Oracle risk and scope** note exists and:
  - [x] lists risks mitigated in M4,
  - [x] lists remaining risks and future work,
  - [x] records deliberate scope limitations (SQL-only v1, no PL/SQL).
- [x] Design or planning documents have been updated if important assumptions changed.

### Evidence to review

- Risk and scope notes.
- Updates to the technical design or planning docs.

### Review questions

1. Do remaining Oracle risks feel like normal maintenance vs. structural hazards?
2. Are future contributors likely to rediscover the same issues, or are they documented well enough?

## 7. Documentation and onboarding

- [x] Documentation:
  - [x] describes the Oracle v1 subset,
  - [x] explains how to run Oracle tests,
  - [x] explains how to run Oracle benchmarks.
- [x] Known Oracle limitations and unsupported features are easy to find.
- [x] Contributor guidance explains how to extend Oracle grammar or mapping (or references `CONTRIBUTING.md`).

### Evidence to review

- `README.md`
- `sql-parser-technical-design.md`
- `milestone-4-oracle-backlog.md`
- any Oracle-specific docs.

### Review questions

1. Can a new contributor work on Oracle with confidence after reading the docs?
2. Are any docs clearly stale after the Milestone 4 implementation?

## 8. Exit criteria for review sign-off

Milestone 4 review is complete when:

- the milestone is classified as **Approved**, **Approved with follow-ups**, or **Changes required**,
- any blocking issues are documented,
- the team agrees the repository is ready to proceed to cross-dialect stabilization (Milestone 5), or to address identified Oracle changes first.
