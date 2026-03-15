# SQL Parser High-Level Implementation Checklist

## Purpose

This checklist captures the high-level implementation path for `sqool`. It is intentionally broad and ordered so each item can later be expanded into a detailed work breakdown, milestone plan, or issue set.

## 1. Foundation and project bootstrap

- [x] Choose the build system: Gradle
- [x] Create the multi-module project structure
- [x] Set Java 25 as the project baseline
- [x] Add ANTLR tool and runtime integration
- [x] Add the testing baseline
- [x] Add the benchmarking baseline with JMH
- [x] Define dependency and version pinning rules
- [x] Establish formatting, static analysis, and CI conventions

### Exit criteria

- The project builds cleanly on Java 25.
- ANTLR code generation runs reproducibly.
- Unit tests and benchmarks can be executed from a clean checkout.

## 2. Public API and core model

- [x] Define the public parser API
- [x] Define the dialect selection model
- [x] Define parse options and parse result types
- [x] Define syntax diagnostics and source span models
- [x] Define parser factory or service loading strategy
- [x] Decide whether comment capture is in or out for the first milestone
- [x] Decide whether script parsing is in the first milestone or follows later

### Exit criteria

- A stable API skeleton exists for all dialects.
- No ANTLR types leak into public interfaces.

## 3. AST v0

- [x] Define the normalized AST root hierarchy
- [x] Define statement nodes for the initial supported subset
- [x] Define expression nodes for the initial supported subset
- [x] Define table, join, ordering, and select item nodes
- [x] Define dialect extension node strategy
- [x] Define source span attachment strategy
- [x] Define AST traversal and visitor conventions
- [x] Define AST golden test format

### Exit criteria

- The AST supports the initial cross-dialect subset without stringly typed escape hatches.
- Dialect-specific constructs can be represented without breaking the shared model.

## 4. Grammar vendoring workflow

- [x] Vendor the selected ANTLR grammars into the repository
- [x] Record upstream source and revision metadata per dialect
- [x] Define local patching rules for upstream grammars
- [x] Confirm grammar generation works for the Java target
- [x] Establish a repeatable grammar update workflow
- [x] Add a regression location for grammar-related fixes

### Exit criteria

- Each dialect grammar has a reproducible source of truth.
- Local grammar deviations are documented and reviewable.

## 5. Parser runtime architecture

- [x] Implement one parser pipeline per dialect
- [x] Implement lexer and token stream setup per dialect
- [x] Implement fast-path SLL parsing
- [x] Implement LL fallback with structured diagnostics
- [x] Disable parse tree creation by default
- [x] Define single-statement entry points
- [x] Define script or multi-statement entry points
- [x] Add internal parser metrics collection

### Exit criteria

- Valid input uses the optimized fast path.
- Failing input produces useful structured syntax diagnostics.

## 6. MySQL implementation

- [x] Import and validate the MySQL grammar
- [x] Implement MySQL parser integration
- [x] Map the initial MySQL subset into the normalized AST
- [x] Add MySQL conformance corpus tests
- [x] Add MySQL syntax error tests
- [x] Add MySQL performance benchmarks against JSqlParser
- [x] Tune MySQL parser hot paths

### Exit criteria

- MySQL is the first end-to-end supported dialect.
- MySQL benchmarks establish the first real performance baseline.

## 7. SQLite implementation

- [x] Import and validate the SQLite grammar
- [x] Implement SQLite parser integration
- [x] Map the initial SQLite subset into the normalized AST
- [x] Add SQLite conformance corpus tests
- [x] Add SQLite syntax error tests
- [x] Add SQLite benchmarks against JSqlParser
- [x] Tune SQLite parser hot paths

### Exit criteria

- SQLite is fully operational through the shared API.
- SQLite confirms the architecture works on a second dialect with limited rework.

## 8. PostgreSQL implementation

- [x] Import the PostgreSQL grammar as an internal fork
- [x] Identify and fix grammar quality and ambiguity issues
- [x] Validate Java-target generation stability
- [x] Implement PostgreSQL parser integration
- [x] Map the initial PostgreSQL subset into the normalized AST
- [x] Add PostgreSQL conformance and regression tests
- [x] Add PostgreSQL benchmarks against JSqlParser
- [x] Optimize ambiguity-heavy rules as needed

### Exit criteria

- PostgreSQL parsing is stable for the supported subset.
- Known grammar cleanup risks are reduced to normal maintenance work.

## 9. Oracle implementation

- [ ] Confirm Oracle v1 scope as SQL-only or broader
- [ ] Import the Oracle grammar base
- [ ] Trim or gate unsupported procedural constructs if needed
- [ ] Implement Oracle parser integration
- [ ] Map the initial Oracle SQL subset into the normalized AST
- [ ] Add Oracle conformance and regression tests
- [ ] Add Oracle benchmarks against JSqlParser
- [ ] Tune Oracle parser hot paths

### Exit criteria

- Oracle SQL is supported within the agreed v1 scope.
- PL/SQL sprawl is explicitly avoided unless it becomes a planned feature.

## 10. Cross-dialect normalization

- [ ] Verify consistent AST behavior across shared SQL constructs
- [ ] Verify consistent diagnostic behavior across dialects
- [ ] Add cross-dialect golden tests for the common subset
- [ ] Reconcile dialect extension nodes with the public API
- [ ] Review naming and package consistency

### Exit criteria

- Consumers can use one API and one core AST model across all supported dialects.

## 11. Performance engineering

- [x] Define the benchmark corpus format
- [x] Build benchmark corpora for all supported dialects
- [x] Establish baseline JSqlParser measurements
- [x] Track throughput, latency, allocations, and GC behavior
- [x] Optimize AST construction overhead
- [x] Optimize fallback and error-path costs
- [x] Review grammar hotspots causing prediction overhead
- [ ] Publish benchmark results in a reproducible form

### Exit criteria

- Performance claims are backed by repeatable benchmark data.
- Bottlenecks are known per dialect and tracked explicitly.

## 12. Conformance and regression quality

- [x] Build a vendor-documentation-based SQL corpus
- [x] Add malformed SQL coverage per dialect
- [x] Add regression tests for every grammar or AST bug fixed
- [x] Add differential tests where practical
- [ ] Define quality gates for adding new syntax coverage

### Exit criteria

- Correctness does not regress as grammar coverage expands.

## 13. Packaging and release readiness

- [ ] Finalize module publication layout
- [ ] Define versioning strategy
- [ ] Finalize documentation for supported dialect coverage
- [ ] Publish benchmark and feature support reports
- [ ] Prepare release automation and CI quality gates
- [ ] Define the upstream grammar refresh process

### Exit criteria

- The library is ready for external consumption and ongoing maintenance.

## 14. Deferred or later-phase items

- [ ] Full Oracle PL/SQL support
- [ ] Comment preservation and trivia-aware AST support
- [ ] Incremental parsing support
- [ ] SQL formatting and linting layers
- [ ] Semantic validation and catalog-aware analysis
- [ ] Dialect-to-dialect transpilation features

## 15. Suggested breakdown order

When this checklist is broken into detailed execution plans, the preferred order is:

1. Foundation and bootstrap
2. Public API and AST v0
3. Grammar vendoring workflow
4. Parser runtime architecture
5. MySQL
6. SQLite
7. PostgreSQL
8. Oracle
9. Cross-dialect normalization
10. Performance hardening
11. Release readiness

## 16. How to use this checklist

This checklist should be used as the top-level planning artifact for:

- milestone definition
- issue decomposition
- sprint planning
- progress reporting
- implementation sequencing

Each checked item should eventually be expanded into:

- a detailed design note,
- a concrete implementation task list, or
- a benchmark or conformance work item.
