# Milestone 1 Backlog – MySQL MVP

## Status

- Draft: v0.1
- Scope: SQL Parser Technical Design – Milestone 1 (MySQL MVP)
- Related docs:
  - `sql-parser-technical-design.md` (§17 Milestones, §10–§13)
  - `sql-parser-implementation-checklist.md` (§6 MySQL implementation)
  - `milestone-0-bootstrap-backlog.md`

## 1. Purpose

This document expands the **Milestone 1: MySQL MVP** line item in the technical design into a concrete, executable implementation backlog.

Milestone 1 should leave the repository in a state where:

- MySQL is the first fully supported dialect end to end through the public API.
- The MySQL parser runs with an SLL-first / LL-fallback pipeline.
- A meaningful subset of MySQL SQL is mapped into the normalized AST.
- There is a conformance and regression corpus for MySQL.
- JMH benchmarks compare `sqool` against JSqlParser on MySQL statements.
- The architecture is strong enough to support later dialects without major rework.

## 2. Recommended baseline decisions

The following decisions are inherited from Milestone 0 and the technical design and are treated as fixed constraints for Milestone 1:

- Keep Java 25 as the baseline, without preview features.
- Maintain the existing module layout (`sqool-core`, `sqool-ast`, `sqool-grammar-mysql`, `sqool-dialect-mysql`, `sqool-conformance`, `sqool-bench`).
- Generate ANTLR sources during the build; do not hand-edit generated code.
- Preserve the narrow, ANTLR-free public API.

Any deviation from these assumptions should be called out explicitly in a design note before implementation.

## 3. Milestone 1 definition of done

Milestone 1 is complete when all of the following are true:

1. The MySQL grammar is vendored, configured, and compiles in `sqool-grammar-mysql`.
2. `sqool-dialect-mysql` exposes a working SLL-first / LL-fallback parser pipeline.
3. A defined initial subset of MySQL SQL is mapped into the normalized AST.
4. The public API can parse MySQL SQL and return `ParseSuccess` / `ParseFailure` with useful diagnostics.
5. JUnit-based conformance and regression tests exist for MySQL.
6. JMH benchmarks compare `sqool` vs JSqlParser on a representative MySQL corpus.
7. MySQL parser behavior and known limitations are documented.

## 4. Work breakdown structure

### Epic M1-1: MySQL grammar vendoring and validation

**Objective**

Bring the real MySQL grammar into `sqool-grammar-mysql` and prove that it generates stable Java parser sources.

**Tasks**

- [x] Confirm the upstream MySQL grammar source and revision.
- [x] Vendor `.g4` grammar files into `sqool-grammar-mysql`.
- [x] Configure grammar source directories for MySQL.
- [x] Configure generated-source output directories (aligned with Milestone 0 conventions).
- [x] Validate Java target generation using the real MySQL grammar.
- [x] Add a minimal parser smoke test that exercises a basic MySQL `SELECT`.
- [x] Document any local patches applied to the upstream grammar (`UPSTREAM.md`).

**Deliverables**

- Vendored MySQL grammar files.
- Working ANTLR generation path for MySQL.
- `UPSTREAM.md` describing provenance and local deviations.

**Dependencies**

- Milestone 0 completed and reviewed.

**Acceptance criteria**

- Running the normal build generates MySQL parser sources without manual steps.
- Generated sources compile successfully.

### Epic M1-2: MySQL parser pipeline integration

**Objective**

Implement the MySQL-specific parser pipeline behind the public `SqlParser` API, with SLL-first / LL-fallback behavior.

**Tasks**

- [x] Implement a MySQL `SqlParser` implementation in `sqool-dialect-mysql`.
- [x] Configure lexer and token stream setup for MySQL, including:
  - [x] input handling (strings, optional script mode),
  - [x] channel and token configuration,
  - [x] error listeners.
- [x] Implement SLL-first parsing with:
  - [x] bail-fast error strategy.
- [x] Implement LL fallback for failed parses with:
  - [x] structured diagnostic collection,
  - [x] richer error messages.
- [x] Wire MySQL parser metrics into `ParseMetrics`.
- [x] Implement single-statement vs script entry points where appropriate.

**Deliverables**

- MySQL dialect implementation wired into `SqlParser`.
- Internal parser metrics visible via `ParseResult`.

**Dependencies**

- M1-1 (grammar vendoring and validation).
- Public API and core model from Milestone 0.

**Acceptance criteria**

- Valid MySQL SQL runs on the SLL fast path in normal use.
- Invalid or unsupported SQL uses the LL fallback and produces structured diagnostics.

### Epic M1-3: MySQL AST mapping

**Objective**

Map a well-defined subset of MySQL constructs into the normalized AST model.

**Tasks**

- [x] Define the initial MySQL v1 subset (e.g.:
  - [x] basic DDL: `CREATE TABLE`, `DROP TABLE`,
  - [x] core DML: `INSERT`, `UPDATE`, `DELETE`,
  - [x] `SELECT` with joins, predicates, and ordering,
  - [x] simple expressions, literals, identifiers).
- [x] Implement visitors or mappers from MySQL parser contexts to AST nodes.
- [x] Implement dialect extension nodes for MySQL-specific syntax that cannot cleanly fit into the shared AST.
- [x] Ensure AST nodes are immutable, compact, and do not retain ANTLR contexts.
- [x] Attach source spans when enabled by `ParseOptions`.
- [x] Add or update AST golden tests covering MySQL constructs.

**Deliverables**

- MySQL AST mapping layer.
- Golden tests for representative MySQL statements.

**Dependencies**

- M1-2 (parser pipeline integration).

**Acceptance criteria**

- The AST supports the agreed v1 MySQL subset without stringly typed escape hatches.
- Dialect-specific constructs are represented via well-defined extension nodes.

### Epic M1-4: MySQL conformance and regression tests

**Objective**

Establish a reproducible conformance and regression test suite for MySQL.

**Tasks**

- [x] Build a MySQL-focused SQL corpus based on:
  - [x] vendor documentation examples,
  - [x] representative application queries,
  - [ ] any early consumer feedback.
- [x] Add conformance tests that:
  - [x] validate parse success for valid queries,
  - [x] validate parse failure and diagnostics for malformed queries,
  - [x] verify AST shapes via golden tests.
- [x] Add regression tests for any bugs found during Milestone 1.
- [x] Organize corpus and test assets under `sqool-conformance`.

**Deliverables**

- MySQL conformance test suite.
- Regression test directory and conventions.

**Dependencies**

- M1-3 (AST mapping) for structural assertions.

**Acceptance criteria**

- A failing grammar or AST change is caught by conformance or regression tests.
- The corpus is easy to extend as new syntax is added.

### Epic M1-5: MySQL performance and benchmark integration

**Objective**

Establish meaningful MySQL performance measurements against JSqlParser.

**Tasks**

- [x] Define a MySQL benchmark corpus (small, medium, large, and error-path statements).
- [x] Implement JMH benchmarks in `sqool-bench` that:
  - [x] parse the corpus with `sqool`,
  - [x] parse the same corpus with JSqlParser where applicable,
  - [x] record throughput, latency, and allocation metrics.
- [x] Capture baseline measurements on a reference machine / CI configuration.
- [x] Document how to run and interpret MySQL benchmarks.

**Deliverables**

- MySQL JMH benchmark classes.
- Documented benchmark commands and expected metrics.

**Dependencies**

- M1-2 (parser pipeline) and M1-3 (AST mapping).

**Acceptance criteria**

- Benchmark runs complete reliably on CI and developer machines.
- Baseline MySQL performance numbers are recorded and can be compared in later milestones.

### Epic M1-6: MySQL documentation and developer experience

**Objective**

Document MySQL support and make it easy for contributors and early adopters to use.

**Tasks**

- [x] Update `README.md` to describe:
  - [x] the current scope of MySQL support,
  - [x] how to run MySQL-specific tests and benchmarks.
- [ ] Add short usage examples for parsing MySQL SQL via the public API.
- [x] Document known limitations, unsupported syntax, and performance caveats.
- [x] Ensure contributor guidelines reference MySQL-specific conventions where necessary.

**Deliverables**

- Updated repository documentation describing Milestone 1 outcomes.

**Dependencies**

- All previous M1 epics.

**Acceptance criteria**

- A new contributor can:
  - build the project,
  - run MySQL tests and benchmarks,
  - understand the current MySQL feature surface and limitations.

## 5. Suggested execution order

The preferred implementation sequence inside Milestone 1 is:

1. M1-1 MySQL grammar vendoring and validation
2. M1-2 MySQL parser pipeline integration
3. M1-3 MySQL AST mapping
4. M1-4 MySQL conformance and regression tests
5. M1-5 MySQL performance and benchmark integration
6. M1-6 MySQL documentation and developer experience

This sequence delivers an end-to-end MySQL path early, then layers on conformance, performance, and documentation.

