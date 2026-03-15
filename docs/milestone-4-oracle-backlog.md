# Milestone 4 Backlog – Oracle SQL MVP

## Status

- **Complete** (implementation and review done; backlog updated to reflect completion).
- Draft: v0.1
- Scope: SQL Parser Technical Design – Milestone 4 (Oracle SQL MVP)
- Related docs:
  - `sql-parser-technical-design.md` (§12 Dialect rollout, §17 Milestones, Oracle scope)
  - `sql-parser-implementation-checklist.md` (§9 Oracle implementation)
  - `milestone-1-mysql-backlog.md`
  - `milestone-2-sqlite-backlog.md`
  - `milestone-3-postgresql-backlog.md`

## 1. Purpose

This document expands the **Milestone 4: Oracle SQL MVP** line item into a concrete implementation backlog.

Milestone 4 should leave the repository in a state where:

- The Oracle grammar is vendored and subset to SQL-first support (no broad PL/SQL).
- A stable Oracle parser pipeline exists behind the shared public API.
- A defined Oracle SQL v1 subset is mapped into the normalized AST.
- Oracle conformance, regression, and benchmark coverage are in place.
- Oracle-specific risks (PL/SQL sprawl, grammar scope) are explicitly controlled.

Oracle is intentionally scoped as **SQL-only for v1**: procedural language blocks, anonymous blocks, and PL/SQL-specific constructs are out of scope unless explicitly added in a later milestone.

## 2. Constraints and scope

Milestone 4 builds on:

- The public API, AST, and parser infrastructure from Milestones 0–3.
- The dialect generalization rules and patterns established by MySQL, SQLite, and PostgreSQL.
- The technical design decision: support Oracle SQL statement parsing; do not promise full PL/SQL support.

Oracle work must:

- Reuse shared components (parser helpers, error listeners, metrics, AST types).
- Keep Oracle-specific behavior isolated to Oracle modules and extension nodes.
- Avoid scope creep into PL/SQL unless it becomes an explicit follow-up milestone.

## 3. Milestone 4 definition of done

Milestone 4 is complete when all of the following are true:

1. The Oracle grammar is vendored into `sqool-grammar-oracle` with clear provenance and SQL-first subsetting.
2. `sqool-dialect-oracle` exposes a parser pipeline that follows the shared SLL/LL pattern.
3. A clearly defined Oracle SQL v1 subset is mapped into the normalized AST.
4. Oracle conformance and regression tests are in place for the supported subset.
5. JMH benchmarks provide baseline metrics for Oracle parsing (vs JSqlParser where applicable).
6. Oracle-specific risks and scope limitations are documented.

## 4. Work breakdown structure

### Epic M4-1: Oracle grammar vendoring and SQL-first subsetting

**Objective**

Vendor the Oracle grammar from grammars-v4 and subset it to SQL-first support, excluding or gating PL/SQL constructs.

**Tasks**

- [x] Identify the upstream Oracle grammar source (e.g. `sql/plsql` in `antlr/grammars-v4`).
- [x] Vendor the grammar into `sqool-grammar-oracle` (lexer/parser `.g4` files).
- [x] Add `UPSTREAM.md` for Oracle including:
  - [x] source repo and path,
  - [x] commit hash,
  - [x] summary of known upstream issues,
  - [x] local goals (SQL-first subset).
- [x] Subset or gate the grammar to exclude:
  - [x] anonymous PL/SQL blocks (`BEGIN ... END`),
  - [x] procedural constructs not needed for SQL statement parsing,
  - [x] or document the subset boundary clearly.
- [x] Configure grammar source directories and generated-source output paths.
- [x] Validate Java target generation and basic compilation.
- [x] Add a minimal Oracle parser smoke test (e.g. simple `SELECT`, basic DDL).

**Deliverables**

- Vendored Oracle grammar under project control.
- `UPSTREAM.md` documenting provenance and SQL-first subset rationale.

**Dependencies**

- Milestones 1–3 substantially complete.

**Acceptance criteria**

- Oracle parser sources are generated and compiled as part of the normal build.
- The grammar subset is clearly documented and does not include broad PL/SQL by default.

### Epic M4-2: Oracle parser pipeline integration

**Objective**

Integrate the Oracle grammar into the shared parser infrastructure behind the `SqlParser` public API.

**Tasks**

- [x] Implement the Oracle dialect implementation in `sqool-dialect-oracle`.
- [x] Use shared parser helpers for:
  - [x] lexer/parser instantiation,
  - [x] token stream and channel configuration,
  - [x] error listeners and diagnostics.
- [x] Implement SLL-first parsing with LL fallback, mirroring the MySQL/SQLite/PostgreSQL pattern.
- [x] Ensure diagnostics for Oracle:
  - [x] include offending token, expected tokens, and spans when enabled,
  - [x] clearly differentiate invalid syntax from unsupported syntax.
- [x] Wire Oracle into `SqlDialect` and `ParseOptions`.
- [x] Record parser metrics via `ParseMetrics`.

**Deliverables**

- Oracle parser pipeline behind the public API.

**Dependencies**

- M4-1 (grammar vendoring).

**Acceptance criteria**

- Valid Oracle SQL uses the SLL-fast path in typical cases.
- Failing input produces structured, useful diagnostics.

### Epic M4-3: Oracle v1 subset definition and AST mapping

**Objective**

Define and implement a stable v1 subset of Oracle SQL mapped into the normalized AST model.

**Tasks**

- [x] Decide and document the Oracle SQL v1 subset (examples, not exhaustive):
  - [x] core DDL (e.g. `CREATE TABLE`, `DROP TABLE`),
  - [x] core DML (`INSERT`, `UPDATE`, `DELETE`),
  - [x] `SELECT` with joins, predicates, ordering,
  - [x] Oracle-specific constructs (e.g. `DUAL`, `ROWNUM`, `NVL`) as extension nodes where needed.
- [x] Implement AST mapping for the v1 subset using:
  - [x] shared mapping helpers for common constructs,
  - [x] Oracle-specific extension nodes for features that cannot be normalized.
- [x] Ensure mapping:
  - [x] does not retain ANTLR contexts,
  - [x] produces immutable AST nodes,
  - [x] attaches source spans correctly when enabled.
- [x] Add AST golden tests for representative Oracle statements in the v1 subset.

**Deliverables**

- Documented Oracle SQL v1 subset.
- Oracle AST mapping implementation and golden tests.

**Dependencies**

- M4-2 (parser pipeline).

**Acceptance criteria**

- The v1 subset is clearly documented and covered by mapping code and tests.
- Oracle-specific constructs are handled via explicit extension nodes.

### Epic M4-4: Oracle conformance and regression tests

**Objective**

Establish an Oracle-focused conformance and regression suite aligned with the v1 subset.

**Tasks**

- [x] Build an Oracle SQL corpus from:
  - [x] Oracle documentation examples,
  - [x] realistic application queries,
  - [x] any bug reports or discovered corner cases.
- [x] Add conformance tests that:
  - [x] assert parse success and AST structure for valid v1 queries,
  - [x] assert diagnostics for malformed or unsupported queries.
- [x] Add regression tests for grammar or mapping bugs found during M4.
- [x] Align directory and naming conventions with MySQL/SQLite/PostgreSQL suites.

**Deliverables**

- Oracle conformance and regression tests under `sqool-conformance`.

**Dependencies**

- M4-3 (AST mapping).

**Acceptance criteria**

- The supported Oracle v1 subset is obvious from tests and corpus.
- Grammar or mapping regressions for the v1 subset are caught by tests.

### Epic M4-5: Oracle performance and benchmarks

**Objective**

Add Oracle benchmarks and capture initial performance characteristics.

**Tasks**

- [x] Define an Oracle benchmark corpus aligned with the v1 subset, plus error-path statements.
- [x] Add JMH benchmarks that:
  - [x] parse the corpus using the Oracle dialect,
  - [x] parse the same corpus using JSqlParser where supported.
- [x] Measure throughput and latency.
- [x] Document how to run Oracle benchmarks and capture baseline.

**Deliverables**

- Oracle benchmark classes in `sqool-bench`.
- Documented Oracle performance baselines (or instructions to capture them).

**Dependencies**

- M4-2 and M4-3.

**Acceptance criteria**

- Oracle benchmark runs are stable and reproducible.
- Any significant performance issues are documented for follow-up.

### Epic M4-6: Oracle risk and scope documentation

**Objective**

Document Oracle-specific risks, scope limitations, and mitigation status.

**Tasks**

- [x] Create a short **Oracle risk and scope** note (new doc or section) that:
  - [x] lists risks mitigated in M4,
  - [x] lists remaining risks and future work,
  - [x] records deliberate scope limitations (SQL-only v1, no PL/SQL).
- [x] Update design or planning docs if assumptions about Oracle grammar or behavior have changed.

**Deliverables**

- Oracle risk and scope documentation.

**Dependencies**

- All previous M4 epics.

**Acceptance criteria**

- The team has a clear picture of Oracle scope and remaining risks going into cross-dialect stabilization.

## 5. Suggested execution order

The preferred implementation sequence inside Milestone 4 is:

1. M4-1 Oracle grammar vendoring and SQL-first subsetting
2. M4-2 Oracle parser pipeline integration
3. M4-3 Oracle v1 subset definition and AST mapping
4. M4-4 Oracle conformance and regression tests
5. M4-5 Oracle performance and benchmarks
6. M4-6 Oracle risk and scope documentation

This sequence ensures the grammar is under control before building the parser pipeline, and correctness and performance are validated before expanding Oracle coverage further.
