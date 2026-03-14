# Milestone 3 Backlog – PostgreSQL Grammar Hardening

## Status

- Draft: v0.1
- Scope: SQL Parser Technical Design – Milestone 3 (PostgreSQL grammar hardening)
- Related docs:
  - `sql-parser-technical-design.md` (§12 Dialect rollout, §17 Milestones, PostgreSQL risks)
  - `sql-parser-implementation-checklist.md` (§8 PostgreSQL implementation)
  - `milestone-1-mysql-backlog.md`
  - `milestone-2-sqlite-backlog.md`

## 1. Purpose

This document expands the **Milestone 3: PostgreSQL grammar hardening** line item into a concrete implementation backlog.

Milestone 3 should leave the repository in a state where:

- The PostgreSQL grammar is vendored as an internal fork and cleaned up for production use.
- Ambiguity-heavy and non-idiomatic grammar constructs are identified, documented, and mitigated.
- A stable PostgreSQL parser pipeline exists behind the shared public API.
- A meaningful PostgreSQL subset is mapped into the normalized AST model.
- PostgreSQL conformance, regression, and benchmark coverage are in place.
- PostgreSQL-specific risks called out in the technical design are reduced to normal maintenance work.

PostgreSQL is intentionally scoped as a **grammar and stability milestone**: depth of feature coverage should be traded off against clarity and robustness of the grammar and parser behavior.

## 2. Constraints and reuse expectations

Milestone 3 builds on:

- The public API, AST, and parser infrastructure from Milestones 0–2.
- The dialect generalization rules (shared parser helpers, mapping helpers, diagnostics).
- Existing MySQL and SQLite patterns for:
  - SLL-first / LL-fallback pipelines,
  - AST mapping for common SQL constructs,
  - diagnostics and source spans.

PostgreSQL work must:

- Reuse shared components wherever possible.
- Keep PostgreSQL-specific behavior isolated to PostgreSQL modules and extension nodes.
- Avoid introducing PostgreSQL-only abstractions when a generalized version would work for future dialects (including Oracle).

## 3. Milestone 3 definition of done

Milestone 3 is complete when all of the following are true:

1. The PostgreSQL grammar is vendored into `sqool-grammar-postgresql` as an **internal fork**, with clear provenance and patch documentation.
2. Ambiguity and quality issues in the upstream grammar are identified and addressed to the extent needed for a stable subset.
3. `sqool-dialect-postgresql` exposes a parser pipeline that follows the shared SLL/LL pattern and integrates with shared parser infrastructure.
4. A clearly defined PostgreSQL v1 subset is mapped into the normalized AST.
5. PostgreSQL conformance and regression tests are in place for the supported subset.
6. JMH benchmarks provide baseline metrics for PostgreSQL parsing (vs JSqlParser where applicable).
7. PostgreSQL-specific risks from the technical design are documented as mitigated or explicitly deferred.

## 4. Work breakdown structure

### Epic M3-1: PostgreSQL grammar vendoring as an internal fork

**Objective**

Vendor the PostgreSQL grammar into `sqool-grammar-postgresql` as an internal fork suitable for long-term, project-specific evolution.

**Tasks**

- [ ] Identify the upstream PostgreSQL grammar source and path in `antlr/grammars-v4`.
- [ ] Vendor the grammar into `sqool-grammar-postgresql` (lexer/parser `.g4` files).
- [ ] Add `UPSTREAM.md` for PostgreSQL including:
  - [ ] source repo,
  - [ ] upstream path,
  - [ ] commit hash,
  - [ ] summary of known upstream issues and local goals.
- [ ] Configure grammar source directories and generated-source output paths.
- [ ] Validate Java target generation and basic compilation.
- [ ] Add a minimal PostgreSQL parser smoke test (e.g., simple `SELECT`, basic DDL).

**Deliverables**

- Vendored PostgreSQL grammar under project control.
- `UPSTREAM.md` documenting provenance and fork rationale.

**Dependencies**

- Milestones 1 and 2 substantially complete (shared infra available).

**Acceptance criteria**

- PostgreSQL parser sources are generated and compiled as part of the normal build.
- The fork is clearly documented and does not depend on live upstream changes.

### Epic M3-2: Grammar quality and ambiguity analysis

**Objective**

Systematically identify, document, and address PostgreSQL grammar quality and ambiguity issues that would hurt stability or performance.

**Tasks**

- [ ] Perform a focused audit of the vendored PostgreSQL grammar to identify:
  - [ ] ambiguous productions,
  - [ ] deeply left-recursive or awkward constructs,
  - [ ] rules that are known to cause performance or prediction issues.
- [ ] Create a short **PostgreSQL Grammar Notes** document (or section) summarizing key findings.
- [ ] Prioritize issues into:
  - [ ] must-fix for the v1 subset,
  - [ ] acceptable but tracked for later,
  - [ ] out of scope.
- [ ] Implement targeted grammar refactors for the must-fix set, preserving correctness while improving predictability.
- [ ] Add regression SQL examples for any fixed ambiguity or quality issue.

**Deliverables**

- Documented list of PostgreSQL grammar risks and mitigation decisions.
- Updated grammar with improved clarity and predictability for the v1 subset.

**Dependencies**

- M3-1 (grammar vendoring).

**Acceptance criteria**

- The most problematic ambiguous/awkward rules have been addressed or isolated.
- Grammar changes are documented and tested via regression cases.

### Epic M3-3: PostgreSQL parser pipeline integration

**Objective**

Integrate the hardened PostgreSQL grammar into the shared parser infrastructure behind the `SqlParser` public API.

**Tasks**

- [ ] Implement the PostgreSQL dialect implementation in `sqool-dialect-postgresql`.
- [ ] Use shared parser helpers for:
  - [ ] lexer/parser instantiation,
  - [ ] token stream and channel configuration,
  - [ ] error listeners and diagnostics.
- [ ] Implement SLL-first parsing with LL fallback, mirroring the MySQL/SQLite pattern.
- [ ] Ensure diagnostics for PostgreSQL:
  - [ ] include offending token, expected tokens, and spans when enabled,
  - [ ] clearly differentiate invalid syntax from unsupported syntax (for the current v1 subset).
- [ ] Wire PostgreSQL into `SqlDialect` and `ParseOptions`.
- [ ] Record parser metrics via `ParseMetrics`.

**Deliverables**

- PostgreSQL parser pipeline behind the public API.

**Dependencies**

- M3-1 and M3-2.

**Acceptance criteria**

- Valid PostgreSQL SQL uses the SLL-fast path in typical cases.
- Failing input produces structured, useful diagnostics.

### Epic M3-4: PostgreSQL v1 subset definition and AST mapping

**Objective**

Define and implement a stable v1 subset of PostgreSQL SQL mapped into the normalized AST model.

**Tasks**

- [ ] Decide and document the PostgreSQL v1 subset (examples, not exhaustive):
  - [ ] core DDL (e.g., `CREATE TABLE`, `DROP TABLE`),
  - [ ] core DML (`INSERT`, `UPDATE`, `DELETE`),
  - [ ] `SELECT` with joins, predicates, and ordering,
  - [ ] basic CTEs and subqueries where feasible,
  - [ ] a minimal set of PostgreSQL-specific constructs (e.g., `RETURNING`) as extension nodes.
- [ ] Implement AST mapping for the v1 subset using:
  - [ ] shared mapping helpers for common constructs,
  - [ ] PostgreSQL-specific extension nodes for features that cannot be normalized.
- [ ] Ensure mapping:
  - [ ] does not retain ANTLR contexts,
  - [ ] produces immutable AST nodes,
  - [ ] attaches source spans correctly when enabled.
- [ ] Add AST golden tests for representative PostgreSQL statements in the v1 subset.

**Deliverables**

- Documented PostgreSQL v1 subset.
- PostgreSQL AST mapping implementation and golden tests.

**Dependencies**

- M3-3 (parser pipeline).

**Acceptance criteria**

- The v1 subset is clearly documented and covered by mapping code and tests.
- PostgreSQL-specific constructs are handled via explicit extension nodes.

### Epic M3-5: PostgreSQL conformance and regression tests

**Objective**

Establish a PostgreSQL-focused conformance and regression suite aligned with the hardened grammar and v1 subset.

**Tasks**

- [ ] Build a PostgreSQL SQL corpus from:
  - [ ] PostgreSQL documentation examples,
  - [ ] realistic application queries,
  - [ ] any bug reports or discovered corner cases.
- [ ] Add conformance tests that:
  - [ ] assert parse success and AST structure for valid v1 queries,
  - [ ] assert diagnostics for malformed or unsupported queries.
- [ ] Add regression tests for:
  - [ ] each grammar ambiguity or bug fixed in M3-2,
  - [ ] any parser or mapping bugs found during M3.
- [ ] Align directory and naming conventions with MySQL/SQLite suites.

**Deliverables**

- PostgreSQL conformance and regression tests under `sqool-conformance` (or equivalent).

**Dependencies**

- M3-4 (AST mapping).

**Acceptance criteria**

- The supported PostgreSQL v1 subset is obvious from tests and corpus.
- Grammar or mapping regressions for the v1 subset are caught by tests.

### Epic M3-6: PostgreSQL performance and benchmarks

**Objective**

Add PostgreSQL benchmarks and capture initial performance characteristics.

**Tasks**

- [ ] Define a PostgreSQL benchmark corpus aligned with the v1 subset, plus error-path statements.
- [ ] Add JMH benchmarks that:
  - [ ] parse the corpus using the PostgreSQL dialect,
  - [ ] parse the same corpus using JSqlParser where supported.
- [ ] Measure:
  - [ ] throughput,
  - [ ] latency,
  - [ ] allocation and GC behavior (where practical).
- [ ] Record baseline PostgreSQL metrics in a reproducible form.
- [ ] Compare PostgreSQL performance to MySQL and SQLite to identify any obvious regressions from grammar complexity.

**Deliverables**

- PostgreSQL benchmark classes in `sqool-bench`.
- Documented PostgreSQL performance baselines and notable findings.

**Dependencies**

- M3-3 and M3-4.

**Acceptance criteria**

- PostgreSQL benchmark runs are stable and reproducible.
- Any significant performance issues attributable to grammar complexity are documented for follow-up.

### Epic M3-7: Risk review and documentation

**Objective**

Explicitly review and document the PostgreSQL-specific risks identified in the technical design, and the current mitigation status.

**Tasks**

- [ ] Map technical-design PostgreSQL risks (e.g., grammar cleanup size, ambiguity) to:
  - [ ] mitigation work completed in M3,
  - [ ] remaining follow-ups with clear priority.
- [ ] Record any deliberate scope limitations for PostgreSQL v1.
- [ ] Update design or planning docs if assumptions about PostgreSQL grammar or behavior have changed.

**Deliverables**

- A short **PostgreSQL risk and mitigation** note (new doc or section).

**Dependencies**

- All previous M3 epics.

**Acceptance criteria**

- The team has a clear picture of remaining PostgreSQL-specific risks going into later milestones.

## 5. Suggested execution order

The preferred implementation sequence inside Milestone 3 is:

1. M3-1 PostgreSQL grammar vendoring as an internal fork
2. M3-2 Grammar quality and ambiguity analysis
3. M3-3 PostgreSQL parser pipeline integration
4. M3-4 PostgreSQL v1 subset definition and AST mapping
5. M3-5 PostgreSQL conformance and regression tests
6. M3-6 PostgreSQL performance and benchmarks
7. M3-7 Risk review and documentation

This sequence ensures that grammar risks are understood early, the parser pipeline is built on a hardened foundation, and correctness and performance are validated before expanding PostgreSQL coverage further.

