# Milestone 2 Backlog – SQLite MVP

## Status

- Draft: v0.1
- Scope: SQL Parser Technical Design – Milestone 2 (SQLite MVP)
- Related docs:
  - `sql-parser-technical-design.md` (§12 Dialect rollout, §17 Milestones)
  - `sql-parser-implementation-checklist.md` (§7 SQLite implementation)
  - `milestone-0-bootstrap-backlog.md`
  - `milestone-1-mysql-backlog.md`

## 1. Purpose

This document expands the **Milestone 2: SQLite MVP** line item into a concrete implementation backlog.

Milestone 2 should leave the repository in a state where:

- SQLite is fully operational through the shared public API.
- The design proven for MySQL is validated on a second dialect with minimal duplication.
- Shared abstractions are extracted where MySQL and SQLite follow the same patterns.
- A SQLite-specific corpus, conformance tests, and benchmarks exist.

SQLite is the first test of the “dialect generalization” strategy described in project rules. It should reuse and, where necessary, generalize the work done for MySQL rather than copy it.

## 2. Constraints and dialect generalization rules

Milestone 2 is constrained by:

- The public API and AST model established by Milestones 0 and 1.
- The module layout including `sqool-grammar-sqlite` and `sqool-dialect-sqlite`.
- The **Dialect generalization** guidance:
  - **Do not** reimplement logic that already exists for MySQL if it can be generalized.
  - **Prefer** extracting shared utilities into `sqool-core` / `sqool-ast` (e.g., parser facades, error listeners, mapping helpers).
  - Treat MySQL as a reference implementation, not a template for copy-paste.

Any deliberate deviations should be documented.

## 3. Milestone 2 definition of done

Milestone 2 is complete when all of the following are true:

1. The SQLite grammar is vendored, configured, and compiles in `sqool-grammar-sqlite`.
2. `sqool-dialect-sqlite` exposes a working parser pipeline that reuses generalized infrastructure where possible.
3. A defined initial subset of SQLite SQL is mapped into the normalized AST.
4. The public API can parse SQLite SQL and return `ParseSuccess` / `ParseFailure` with useful diagnostics.
5. Conformance and regression tests exist for SQLite.
6. JMH benchmarks compare `sqool` vs JSqlParser (where applicable) on SQLite statements.
7. Duplicate logic between MySQL and SQLite has been factored into shared components where appropriate.

## 4. Work breakdown structure

### Epic M2-1: Dialect generalization from MySQL

**Objective**

Identify MySQL-specific parser and mapper logic that should be generalized for reuse in SQLite and later dialects.

**Tasks**

- [x] Survey `sqool-dialect-mysql` and related helpers for:
  - [x] parser setup and configuration,
  - [x] error listener and diagnostic construction,
  - [x] AST mapping patterns for common SQL constructs,
  - [x] source-span handling and metrics.
- [ ] Propose a short design note for:
  - [ ] `AntlrParserFacade` or equivalent shared parser entry abstraction,
  - [ ] shared error listener / diagnostic utilities,
  - [ ] shared mapping helpers for expressions, literals, identifiers, and common clauses.
- [ ] Refactor MySQL to use the new shared abstractions without changing observable behavior.
- [ ] Identify clearly which parts remain dialect-specific and why.

*Note: Both dialects use `AntlrSyntaxErrorListener`, `ParseAttempt`, `SourceSpans`, and `ParseMetrics` from sqool-core, but each has its own parser implementation. No `AntlrParserFacade` abstraction exists yet.*

**Deliverables**

- Generalized utilities in `sqool-core` / `sqool-ast` (or focused shared modules).
- Updated MySQL implementation using shared components.

**Dependencies**

- Milestone 1 substantially complete and stable.

**Acceptance criteria**

- The MySQL implementation remains correct and passes all existing tests.
- New shared components are obviously reusable by SQLite.

### Epic M2-2: SQLite grammar vendoring and validation

**Objective**

Vendor and validate the SQLite grammar in `sqool-grammar-sqlite`.

**Tasks**

- [x] Confirm the upstream SQLite grammar source and revision.
- [x] Vendor `.g4` grammar files into `sqool-grammar-sqlite`.
- [x] Configure grammar source directories for SQLite.
- [x] Configure generated-source output directories.
- [x] Validate Java target generation and ensure SQLite grammar compiles.
- [x] Add a minimal SQLite parser smoke test (e.g., a basic `SELECT` or DDL).
- [x] Add `UPSTREAM.md` documenting provenance and local patches.

**Deliverables**

- Vendored SQLite grammar.
- Working ANTLR generation path for SQLite with documentation.

**Dependencies**

- M2-1 (where shared build configuration is affected).

**Acceptance criteria**

- SQLite parser sources are generated and compiled as part of the normal build.

### Epic M2-3: SQLite parser pipeline integration

**Objective**

Implement the SQLite-specific parser pipeline behind the public API, reusing generalized infrastructure from Milestone 1.

**Tasks**

- [x] Implement a SQLite `SqlParser` dialect implementation in `sqool-dialect-sqlite`.
- [x] Configure lexer and token stream setup for SQLite via shared parser utilities where appropriate.
- [x] Implement SLL-fast / LL-fallback behavior using the same pattern as MySQL.
- [x] Ensure diagnostics construction reuses shared error helper code.
- [x] Wire SQLite into `ParseOptions` / `SqlDialect`.
- [x] Add parser metrics for SQLite.

**Deliverables**

- SQLite dialect implementation wired into `SqlParser`.

**Dependencies**

- M2-1 (shared abstractions),
- M2-2 (grammar vendoring and validation).

**Acceptance criteria**

- Valid SQLite SQL uses an SLL-fast path.
- Failing or unsupported SQLite SQL produces structured diagnostics comparable to MySQL.

### Epic M2-4: SQLite AST mapping

**Objective**

Map an initial SQLite SQL subset into the normalized AST model, reusing existing mappings for shared constructs.

**Tasks**

- [x] Define the SQLite v1 subset (e.g.:
  - [x] `SELECT` with joins, predicates, ordering → normalized `SelectStatement`,
  - [x] `CREATE TABLE`, `DROP TABLE`, `INSERT`, `UPDATE`, `DELETE` → `SqliteRawStatement` extension nodes (not yet normalized),
  - [ ] SQLite-specific conflict resolution clauses as extension nodes).
- [x] Implement SQLite mapping in terms of:
  - [x] shared `SourceSpans` and AST types from sqool-ast,
  - [x] SQLite-specific extension nodes where needed (`SqliteRawStatement`).
- [x] Ensure AST nodes remain immutable, compact, and ANTLR-free.
- [x] Attach source spans for SQLite when enabled.
- [x] Add or extend AST golden tests to cover SQLite statements (`SqliteSqlParserTest`).

**Deliverables**

- SQLite AST mapping layer.
- Golden tests for representative SQLite SQL.

**Dependencies**

- M2-3 (parser pipeline).

**Acceptance criteria**

- Shared constructs (e.g., generic `SELECT`, expressions) produce consistent AST shapes across MySQL and SQLite.
- SQLite-specific constructs are modeled via clear extension nodes.

### Epic M2-5: SQLite conformance and regression tests

**Objective**

Establish a SQLite-specific conformance and regression suite.

**Tasks**

- [x] Build a SQLite SQL corpus from:
  - [x] SQLite documentation examples,
  - [x] realistic application queries,
- [x] Add conformance tests that:
  - [x] assert parse success for valid queries,
  - [x] assert parse failure and diagnostics for malformed queries,
  - [x] verify AST structures where relevant.
- [x] Add regression tests for any SQLite issues found during implementation.
- [x] Align directory and naming conventions with the MySQL conformance suite.

**Deliverables**

- SQLite conformance and regression tests under `sqool-conformance` (or equivalent).

**Dependencies**

- M2-4 (AST mapping).

**Acceptance criteria**

- The supported SQLite subset is obvious from the corpus and tests.
- New syntax additions can be accompanied by SQLite tests with minimal friction.

### Epic M2-6: SQLite performance and benchmarks

**Objective**

Add SQLite benchmarks and compare `sqool` to JSqlParser where appropriate.

**Tasks**

- [x] Define a SQLite benchmark corpus with small, medium, large, and error-path statements.
- [x] Add JMH benchmarks that:
  - [x] parse the SQLite corpus via `sqool`,
  - [x] parse via JSqlParser when the same statements are supported.
- [x] Measure throughput, latency, and allocations.
- [ ] Capture baseline SQLite benchmark results in a reproducible form.
- [x] Document how to run SQLite benchmarks.

**Deliverables**

- SQLite benchmark classes in `sqool-bench`.
- Documented SQLite performance baselines.

**Dependencies**

- M2-3 (pipeline) and M2-4 (AST mapping).

**Acceptance criteria**

- SQLite benchmarks run reliably on CI / developer machines.
- Performance numbers can be compared across MySQL and SQLite.

### Epic M2-7: Cross-dialect consistency checks (MySQL vs SQLite)

**Objective**

Validate that MySQL and SQLite behave consistently for shared constructs and that dialect generalization is working.

**Tasks**

- [x] Add cross-dialect tests for:
  - [x] identical `SELECT`-style queries,
  - [x] common expression trees,
  - [x] basic DDL/DML overlap.
- [x] Verify that:
  - [x] AST shapes match for shared constructs,
  - [x] diagnostics are consistent across dialects for analogous errors.
- [ ] Review naming, packaging, and shared helper APIs for clarity.

**Deliverables**

- Cross-dialect tests (MySQL vs SQLite) in `sqool-conformance` or a dedicated test package.

**Dependencies**

- M1 (MySQL) and all earlier M2 epics.

**Acceptance criteria**

- Consumers can reasonably treat MySQL and SQLite as using one coherent AST and diagnostic model.

## 5. Suggested execution order

The preferred implementation sequence inside Milestone 2 is:

1. M2-1 Dialect generalization from MySQL
2. M2-2 SQLite grammar vendoring and validation
3. M2-3 SQLite parser pipeline integration
4. M2-4 SQLite AST mapping
5. M2-5 SQLite conformance and regression tests
6. M2-6 SQLite performance and benchmarks
7. M2-7 Cross-dialect consistency checks

This sequence ensures that shared abstractions are in place before deep SQLite work begins, and that cross-dialect behavior is validated before moving on to later dialects.

