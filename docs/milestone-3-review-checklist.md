# Milestone 3 Review Checklist – PostgreSQL Grammar Hardening

## Purpose

This checklist is for reviewing the **Milestone 3: PostgreSQL grammar hardening** implementation. It validates that the PostgreSQL grammar has been safely forked and improved, that the PostgreSQL parser is stable for a defined v1 subset, and that the project is ready to build on this foundation.

The review should focus on:

- quality and maintainability of the vendored PostgreSQL grammar,
- stability and predictability of the PostgreSQL parser pipeline,
- correctness and clarity of AST mapping for the v1 subset,
- depth of PostgreSQL conformance and regression coverage,
- PostgreSQL performance characteristics and documented risks.

The review should not spend much time on:

- full PostgreSQL feature completeness (beyond the agreed v1 subset),
- Oracle-specific or cross-dialect normalization that is not yet targeted by this milestone.

## Recommended review outcome

At the end of the review, classify Milestone 3 as one of:

- [ ] **Approved**: PostgreSQL grammar and parser are stable enough to proceed to Oracle / cross-dialect stabilization.
- [ ] **Approved with follow-ups**: M3 is acceptable, but some non-blocking improvements should be addressed.
- [ ] **Changes required**: structural or correctness issues must be fixed before starting the next milestone.

Reference documents:

- `sql-parser-technical-design.md` (PostgreSQL-related sections and risks)
- `sql-parser-implementation-checklist.md` (§8)
- `milestone-3-postgresql-backlog.md`
- any PostgreSQL-specific notes (`UPSTREAM.md`, grammar notes, risk notes)

## 1. PostgreSQL grammar vendoring and fork quality

- [ ] PostgreSQL grammar is vendored into `sqool-grammar-postgresql` as an internal fork.
- [ ] `UPSTREAM.md` (or equivalent) documents:
  - [ ] upstream repository and path,
  - [ ] chosen commit hash,
  - [ ] known upstream issues,
  - [ ] local goals for the fork.
- [ ] Grammar source layout (lexer/parser `.g4` files) is clear and consistent with other grammar modules.
- [ ] ANTLR generation for PostgreSQL is integrated into the normal Gradle build.
- [ ] Generated sources compile cleanly without manual edits.

### Evidence to review

- `sqool-grammar-postgresql` directory contents.
- ANTLR configuration in `build.gradle.kts` for the PostgreSQL module.
- `UPSTREAM.md` for PostgreSQL.

### Review questions

1. Is the fork boundary clear enough to support long-term evolution?
2. Are any local patches too invasive or insufficiently documented?

## 2. Grammar quality and ambiguity handling

- [ ] A structured review of grammar quality and ambiguity has been performed.
- [ ] There is a short **PostgreSQL grammar notes** or similar document summarizing:
  - [ ] key ambiguous or awkward rules,
  - [ ] refactorings applied in Milestone 3,
  - [ ] known remaining issues and their priority.
- [ ] The most problematic ambiguity hotspots have been addressed or deliberately scoped out of the v1 subset.
- [ ] Regression SQL examples exist for each significant grammar change.

### Evidence to review

- Grammar notes / analysis document.
- Diffs or summaries of grammar changes vs upstream.
- Regression tests tied to specific grammar fixes.

### Review questions

1. Are the main PostgreSQL grammar risks materially reduced after M3?
2. Are remaining issues clearly described so future work is straightforward?

## 3. PostgreSQL parser pipeline

- [x] PostgreSQL dialect implementation resides in `sqool-dialect-postgresql`.
- [x] Parser setup follows the established pattern:
  - [x] SLL-fast path with parse tree disabled and bail-fast error strategy,
  - [x] LL-fallback path with structured diagnostics.
- [x] Shared parser utilities are used wherever reasonable (lexer/parser setup, error listeners, metrics).
- [x] Single-statement vs script entry points are defined where applicable.
- [x] `SqlDialect` and `ParseOptions` support PostgreSQL cleanly.
- [x] `ParseMetrics` capture PostgreSQL behavior for benchmarking and debugging.

### Evidence to review

- `sqool-dialect-postgresql` code.
- Any shared parser helper abstractions used by MySQL, SQLite, and PostgreSQL.
- Tests that exercise both successful and failing PostgreSQL parses.

### Review questions

1. Is the PostgreSQL parsing flow understandable and maintainable?
2. Are error messages and diagnostics up to the standard set by MySQL/SQLite?

## 4. PostgreSQL v1 subset and AST mapping

- [ ] The PostgreSQL v1 subset is explicitly defined (in docs or code comments).
- [ ] For the v1 subset:
  - [ ] AST mapping covers basic DDL, DML, and `SELECT`-style queries,
  - [ ] common constructs reuse the normalized AST model and shared helpers,
  - [ ] PostgreSQL-specific constructs (e.g., `RETURNING`) are represented via extension nodes.
- [ ] AST nodes remain:
  - [ ] immutable,
  - [ ] compact,
  - [ ] free of ANTLR context references.
- [ ] Source spans are attached properly when `ParseOptions` request them.
- [ ] Golden tests exist for representative PostgreSQL statements in the v1 subset.

### Evidence to review

- PostgreSQL mapping code in `sqool-dialect-postgresql`.
- AST node definitions in `sqool-ast`.
- Golden test assets and their serialized forms.

### Review questions

1. Is the v1 subset well-scoped and realistic?
2. Are PostgreSQL-specific features modeled in a way that will scale to later expansion?

## 5. PostgreSQL conformance and regression quality

- [ ] A PostgreSQL SQL corpus exists and is:
  - [ ] sourced from vendor docs and realistic examples,
  - [ ] organized clearly with version control.
- [ ] Conformance tests:
  - [ ] cover core v1 constructs,
  - [ ] verify both successful parses and expected failures,
  - [ ] validate AST structures where appropriate.
- [ ] Regression tests:
  - [ ] cover each grammar ambiguity or mapping bug fixed during M3,
  - [ ] are easy to extend when new issues are discovered.
- [ ] Test structure is consistent with MySQL and SQLite suites.

### Evidence to review

- PostgreSQL test suites and SQL files under `sqool-conformance` (or equivalent).
- Regression test files and naming conventions.

### Review questions

1. Is it easy to see what PostgreSQL features are supported today?
2. Are tests likely to catch future regressions in the v1 subset?

## 6. PostgreSQL performance and benchmarks

- [ ] A PostgreSQL benchmark corpus is defined for:
  - [ ] small,
  - [ ] medium,
  - [ ] large/complex,
  - [ ] error-path statements.
- [ ] JMH benchmarks in `sqool-bench` exercise PostgreSQL parsing using:
  - [ ] the `sqool` PostgreSQL dialect,
  - [ ] JSqlParser where the same statements are supported.
- [ ] Benchmarks report:
  - [ ] throughput,
  - [ ] latency,
  - [ ] allocation/GC behavior (where practical).
- [ ] Baseline PostgreSQL metrics are recorded in a way that allows comparison with:
  - [ ] MySQL,
  - [ ] SQLite,
  - [ ] future PostgreSQL optimizations.

### Evidence to review

- PostgreSQL benchmark classes and Gradle configuration.
- Stored or documented benchmark results.

### Review questions

1. Are any PostgreSQL-specific performance pathologies obvious from the data?
2. Is there a clear plan for addressing performance issues, if present?

## 7. Risk review and documentation

- [ ] PostgreSQL-specific risks from the technical design (e.g., grammar cleanup scope, ambiguity) have been reviewed against actual M3 work.
- [ ] A short **PostgreSQL risk and mitigation** note exists and:
  - [ ] lists risks mitigated in M3,
  - [ ] lists remaining risks and future work.
- [ ] Any deliberate scope limitations for PostgreSQL v1 are clearly recorded and justified.
- [ ] Design or planning documents have been updated if important assumptions changed.

### Evidence to review

- Risk and mitigation notes.
- Updates to the technical design or planning docs.

### Review questions

1. Do remaining PostgreSQL risks feel like normal maintenance vs. structural hazards?
2. Are future contributors likely to rediscover the same issues, or are they documented well enough?

## 8. Documentation and onboarding

- [ ] Documentation:
  - [ ] describes the PostgreSQL v1 subset,
  - [ ] explains how to run PostgreSQL tests,
  - [ ] explains how to run PostgreSQL benchmarks.
- [ ] Known PostgreSQL limitations and unsupported features are easy to find.
- [ ] Contributor guidance explains:
  - [ ] how to extend PostgreSQL grammar or mapping,
  - [ ] how to add tests and benchmarks for new features.

### Evidence to review

- `README.md`
- `sql-parser-technical-design.md`
- `sql-parser-implementation-checklist.md`
- `milestone-3-postgresql-backlog.md`
- any PostgreSQL-specific docs.

### Review questions

1. Can a new contributor work on PostgreSQL with confidence after reading the docs?
2. Are any docs clearly stale after the Milestone 3 implementation?

## 9. Exit criteria for review sign-off

Milestone 3 review is complete when:

- the milestone is classified as **Approved**, **Approved with follow-ups**, or **Changes required**,
- any blocking issues are documented,
- the team agrees the repository is ready to proceed to Oracle SQL MVP and/or cross-dialect stabilization, or to address identified PostgreSQL changes first.

