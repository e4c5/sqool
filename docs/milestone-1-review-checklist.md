# Milestone 1 Review Checklist – MySQL MVP

## Purpose

This checklist is for reviewing the **Milestone 1: MySQL MVP** implementation. It is meant to validate that MySQL is the first solid, end-to-end supported dialect before expanding to SQLite or other dialects.

The review should focus on:

- MySQL grammar vendoring and ANTLR integration
- MySQL parser pipeline (SLL-fast / LL-fallback)
- AST mapping for the initial MySQL subset
- Conformance, regression, and benchmark coverage
- Public API behavior and diagnostics for MySQL

The review should not spend much time on:

- non-MySQL dialect coverage,
- full cross-dialect normalization (Milestone 5),
- advanced performance tuning beyond establishing clear baselines,
- long-term Oracle / PostgreSQL feature scope.

## Recommended review outcome

At the end of the review, classify Milestone 1 as one of:

- [ ] **Approved**: MySQL MVP is strong enough to start SQLite work.
- [ ] **Approved with follow-ups**: MySQL MVP is acceptable, but a few non-blocking issues should be fixed soon.
- [ ] **Changes required**: one or more structural or correctness issues must be addressed before starting Milestone 2.

Reference documents:

- `sql-parser-technical-design.md` (§10–§13, §17)
- `sql-parser-implementation-checklist.md` (§6)
- `milestone-1-mysql-backlog.md`

## 1. MySQL grammar vendoring and ANTLR integration

- [ ] The MySQL grammar is vendored into `sqool-grammar-mysql` with clear provenance.
- [ ] `UPSTREAM.md` or equivalent documentation records:
  - [ ] upstream source repo and path,
  - [ ] commit hash,
  - [ ] local patches and rationale.
- [ ] ANTLR generation for MySQL runs as part of the normal Gradle build.
- [ ] Generated sources are not hand-edited and compile cleanly.
- [ ] The grammar layout is compatible with formatting and static analysis rules.

### Evidence to review

- MySQL grammar files in `sqool-grammar-mysql`.
- ANTLR configuration in `build.gradle.kts` for the MySQL grammar module.
- `UPSTREAM.md` for MySQL.
- MySQL parser smoke tests.

### Review questions

1. Are local grammar patches minimal and well-documented?
2. Does the current grammar layout scale to additional MySQL syntax coverage?

## 2. MySQL parser pipeline (SLL-first, LL-fallback)

- [ ] The MySQL parser implementation lives in `sqool-dialect-mysql`.
- [ ] Lexer and token stream setup is clearly encapsulated and reusable.
- [ ] The SLL-fast path is configured with:
  - [ ] parse tree creation disabled,
  - [ ] a bail-fast error strategy.
- [ ] The LL-fallback path is configured with:
  - [ ] structured diagnostic collection,
  - [ ] richer error messages.
- [ ] Single-statement and script entry points are clearly separated where relevant.
- [ ] Parser metrics (e.g., prediction mode, timing) are wired into `ParseMetrics`.

### Evidence to review

- `sqool-dialect-mysql` implementation.
- Any shared parser helper utilities in `sqool-core`.
- Tests that assert correct fast-path vs fallback behavior.

### Review questions

1. Is the parsing flow easy to understand and maintain?
2. Are there any obvious sources of unnecessary allocation or overhead?

## 3. MySQL AST mapping and dialect extensions

- [ ] The initial MySQL v1 subset is defined and documented.
- [ ] AST mappings cover:
  - [ ] basic DDL (`CREATE TABLE`, `DROP TABLE`),
  - [ ] core DML (`INSERT`, `UPDATE`, `DELETE`),
  - [ ] `SELECT` with joins, predicates, ordering,
  - [ ] common expressions, literals, identifiers.
- [ ] AST nodes:
  - [ ] are immutable (e.g., records),
  - [ ] store only semantically relevant fields,
  - [ ] do not retain ANTLR contexts,
  - [ ] optionally carry source spans.
- [ ] MySQL-specific constructs use explicit dialect extension nodes instead of string payloads.
- [ ] AST golden tests exist for representative MySQL statements.

### Evidence to review

- AST node definitions in `sqool-ast`.
- MySQL AST mapping code in `sqool-dialect-mysql` or shared helpers.
- Golden tests and their serialized representations.

### Review questions

1. Is the AST mapping consistent with the normalized model and future dialect needs?
2. Are dialect extension nodes clearly separated from the shared core?

## 4. MySQL conformance and regression quality

- [ ] A MySQL SQL corpus exists and is:
  - [ ] organized, versioned, and reproducible,
  - [ ] clearly sourced (vendor docs, examples, etc.).
- [ ] Conformance tests assert:
  - [ ] parse success for valid queries,
  - [ ] parse failure and diagnostics for malformed queries,
  - [ ] structural AST expectations where appropriate.
- [ ] Regression tests are added for any MySQL bugs discovered during implementation.
- [ ] Test naming and directory layout make it easy to add more coverage.

### Evidence to review

- MySQL tests and SQL files under `sqool-conformance`.
- Regression test locations and naming conventions.

### Review questions

1. Is it easy to see what subset of MySQL is currently supported?
2. Will new syntax additions naturally come with tests under the current structure?

## 5. MySQL performance and benchmarks

- [ ] A MySQL benchmark corpus is defined with:
  - [ ] small statements,
  - [ ] medium statements,
  - [ ] large / complex statements,
  - [ ] error-path statements.
- [ ] JMH benchmarks in `sqool-bench` exercise the MySQL corpus using:
  - [ ] `sqool`,
  - [ ] JSqlParser (where applicable).
- [ ] Benchmarks report:
  - [ ] throughput (operations per second),
  - [ ] latency,
  - [ ] allocation and GC behavior (where available).
- [ ] Baseline numbers are captured in a way that can be compared over time.
- [ ] Running MySQL benchmarks is documented for contributors.

### Evidence to review

- JMH benchmark classes for MySQL.
- Benchmark configuration in `build.gradle.kts`.
- Any stored or documented benchmark results.

### Review questions

1. Are the benchmarks realistic enough to catch regressions?
2. Is the benchmark setup too heavy for regular use, or appropriate?

## 6. Public API behavior and diagnostics for MySQL

- [ ] The public API (`SqlParser`, `ParseOptions`, `ParseResult`, diagnostics model) works end to end for MySQL.
- [ ] No ANTLR types leak into public interfaces.
- [ ] Diagnostics are:
  - [ ] structured,
  - [ ] informative (offending token, expected tokens, spans when enabled),
  - [ ] clear about unsupported vs invalid syntax.
- [ ] The API is narrow and predictable for calling code.

### Evidence to review

- Public API types in `sqool-core` and `sqool-ast`.
- Integration or smoke tests that use the public API directly with MySQL SQL.

### Review questions

1. Would a typical consumer find the MySQL API easy to use?
2. Are error messages and diagnostics good enough for real-world debugging?

## 7. Documentation and onboarding

- [ ] The README or related docs:
  - [ ] describe the current MySQL feature surface,
  - [ ] explain how to run MySQL tests,
  - [ ] explain how to run MySQL benchmarks.
- [ ] Known limitations and unsupported MySQL features are clearly documented.
- [ ] Contributor expectations for touching MySQL code are clear (tests, benchmarks, formatting).

### Evidence to review

- `README.md`
- `sql-parser-technical-design.md`
- `sql-parser-implementation-checklist.md`
- `milestone-1-mysql-backlog.md`

### Review questions

1. Can a new contributor get productive on MySQL work after reading the docs?
2. Are any planning documents now stale after the Milestone 1 implementation?

## 8. Exit criteria for review sign-off

Milestone 1 review is complete when:

- the milestone is classified as **Approved**, **Approved with follow-ups**, or **Changes required**,
- any blocking issues are clearly documented,
- the team agrees the repository is ready to begin **Milestone 2: SQLite MVP** (or to first address required MySQL changes).

