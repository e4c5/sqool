# SQL Parser Technical Design

## Status

- Draft: v0.1
- Target runtime: Java 25
- Parser generator: ANTLR 4
- Primary goal: build a high-performance SQL parser library for MySQL, PostgreSQL, Oracle, and SQLite
- Implementation status: MySQL, SQLite, and PostgreSQL have MVP implementations; Oracle is planned

## 1. Overview

This document defines the technical design for `sqool`, an ANTLR-based SQL parser written in Java 25. The parser will support four major dialect families:

- MySQL
- PostgreSQL
- Oracle
- SQLite

The implementation will favor:

- high parse throughput
- low allocation rate
- precise syntax diagnostics
- a stable normalized AST
- dialect-specific parsing behind a unified public API

The design treats correctness and benchmarked performance as first-class requirements. "Faster than JSqlParser" is a measurable target, not an assumption.

## 2. Goals and Non-Goals

### Goals

1. Parse SQL statements for MySQL, PostgreSQL, Oracle, and SQLite.
2. Expose one public API with a consistent result model across dialects.
3. Produce a normalized AST for supported constructs.
4. Preserve dialect-specific syntax via extension nodes where normalization would lose fidelity.
5. Outperform JSqlParser on defined benchmark suites for at least the v1 supported statement classes.
6. Use modern Java 25 language features where they improve maintainability or measurable runtime behavior.
7. Keep grammar acquisition reproducible and maintainable.

### Non-Goals for v1

1. Semantic validation against a catalog or schema.
2. Query optimization or execution planning.
3. Dialect transpilation.
4. Full procedural language support for Oracle PL/SQL.
5. Incremental editor parsing.
6. SQL formatting or linting, except where basic AST fidelity enables later extensions.

## 3. Upstream Grammar Selection

The parser will source initial grammars from `antlr/grammars-v4`, with dialect-specific handling based on maturity and risk.

### 3.1 Selected grammar bases

| Dialect | Upstream path | Planned use | Notes |
| --- | --- | --- | --- |
| MySQL | `sql/mysql/Oracle` (grammars-v4) | Direct import, then harden | Best provenance and likely the fastest route to a stable parser. |
| PostgreSQL | `sql/postgresql` (grammars-v4) | Import and fork | Known grammar quality issues require cleanup before production use. |
| Oracle | `sql/plsql` (grammars-v4) | Import, then subset or gate features | Use Oracle SQL first; defer broad PL/SQL support. |
| SQLite | `bkiers/sqlite-parser` | Direct import, then validate | Community grammar; see `sqool-grammar-sqlite/UPSTREAM.md`. |

### 3.2 Grammar policy

1. Vendor grammar sources into the repository rather than depending on remote generation.
2. Preserve an `UPSTREAM.md` note per dialect with:
   - source repository
   - upstream path
   - commit hash
   - local deviations
3. Prefer target-agnostic grammar changes.
4. Keep grammar actions to an absolute minimum.
5. Treat PostgreSQL as an internal fork from the start.

## 4. Architecture

### 4.1 High-level approach

The library will use:

- one lexer/parser grammar pair per dialect
- one public API
- one normalized AST model
- dialect extension nodes for syntax that cannot be represented cleanly in the shared core

This design reduces parse ambiguity and gives each dialect room for targeted optimization. It also avoids the performance and maintenance risk of a single mega-grammar.

### 4.2 Parse pipeline

For a single parse request:

1. Select dialect parser implementation.
2. Lex input into a token stream.
3. Attempt fast-path parse with:
   - SLL prediction
   - bail-fast error strategy
   - parse tree built (required for AST mapping; future optimization may explore alternatives)
4. If the fast path fails with a recoverable syntax issue, retry with:
   - LL prediction
   - structured error collection
   - richer diagnostics
5. Map parser contexts directly into the normalized AST.
6. Return a `ParseResult` containing either:
   - an AST, or
   - one or more syntax errors

### 4.3 Why SLL-first

ANTLR parsers can perform well when the happy path avoids expensive recovery and unnecessary parse tree construction. The default execution model will therefore optimize for valid input:

- valid SQL should stay on the SLL fast path
- invalid SQL should incur the fallback cost only when needed

## 5. Java 25 Design Choices

The implementation will target Java 25 while avoiding preview-only dependencies in v1.

### 5.1 Language features to use

- `record` for immutable value types and small AST nodes
- sealed interfaces for AST hierarchies
- pattern matching for `switch` in AST visitors, rewriters, and printers
- modern `List`, `Map`, and stream usage where it improves clarity without creating hot-path overhead

### 5.2 Runtime-level expectations

The project will benefit from Java 25 JVM improvements automatically, but parser design must still avoid avoidable allocation and ambiguity. The language version is an enabler, not the primary source of speed.

### 5.3 Initial rule

Use only standard, non-preview Java 25 language features in the initial implementation. Reconsider preview adoption only if benchmark data shows a meaningful advantage.

## 6. Repository and Module Layout

The project should be a multi-module build from the outset.

### 6.1 Proposed modules

```text
sqool-root
|- sqool-core
|- sqool-ast
|- sqool-grammar-mysql
|- sqool-grammar-postgresql
|- sqool-grammar-oracle
|- sqool-grammar-sqlite
|- sqool-dialect-mysql
|- sqool-dialect-postgresql
|- sqool-dialect-oracle
|- sqool-dialect-sqlite
|- sqool-conformance
`- sqool-bench
```

### 6.2 Module responsibilities

#### `sqool-core`

- public API
- parse options
- parser factory
- error model
- source span model
- dialect enum

#### `sqool-ast`

- normalized AST
- common node interfaces
- dialect extension node contracts
- AST visitor interfaces

#### `sqool-grammar-*`

- vendored `.g4` files
- generated ANTLR sources
- grammar-specific wrappers

#### `sqool-dialect-*`

- dialect parser entry points
- token stream configuration
- fallback strategy
- AST mapping from parser contexts
- dialect-specific error interpretation

#### `sqool-conformance`

- corpus tests
- regression SQL files
- AST golden tests
- malformed SQL coverage

#### `sqool-bench`

- JMH benchmarks
- corpus loaders
- JSqlParser comparisons
- allocation and latency measurement

## 7. Public API Design

The public API should stay narrow and predictable.

### 7.1 Core types

```java
public enum SqlDialect {
    MYSQL,
    POSTGRESQL,
    ORACLE,
    SQLITE
}

public record ParseOptions(
        SqlDialect dialect,
        boolean scriptMode,
        boolean includeSourceSpans,
        boolean enableFallback
) {}

/** Metrics captured during a parse (prediction mode, elapsed time). */
public record ParseMetrics(PredictionMode predictionMode, long parseTimeNanos) {}
public enum PredictionMode { SLL, LL, UNKNOWN }

public sealed interface ParseResult permits ParseSuccess, ParseFailure {}

public record ParseSuccess(
        SqlDialect dialect,
        AstNode root,
        List<SyntaxDiagnostic> diagnostics,
        ParseMetrics metrics
) implements ParseResult {}

public record ParseFailure(
        SqlDialect dialect,
        List<SyntaxDiagnostic> diagnostics,
        ParseMetrics metrics
) implements ParseResult {}
```

### 7.2 Service API

```java
public interface SqlParser {
    ParseResult parse(String sql, ParseOptions options);
}
```

### 7.3 Separate entry points

Internally, the dialect implementations should distinguish between:

- single statement parsing
- multi-statement script parsing

This avoids paying for generalized multi-statement handling on the single-statement hot path.

## 8. AST Design

### 8.1 Design principles

The AST must be:

- immutable
- compact
- traversal-friendly
- stable across parser revisions
- explicit about source locations when enabled

### 8.2 Core hierarchy

At the highest level:

```java
public sealed interface AstNode permits Statement, Expression, TableReference, OrderItem, SelectItem {}

public sealed interface Statement extends AstNode
    permits CreateDatabaseStatement,
        CreateTableStatement,
        DeleteStatement,
        DropDatabaseStatement,
        DropTableStatement,
        InsertStatement,
        MySqlRawStatement,
        PostgresqlRawStatement,
        ReplaceStatement,
        SelectStatement,
        SetOperationStatement,
        ShowStatement,
        SqliteRawStatement,
        TruncateTableStatement,
        UpdateStatement {}

public sealed interface Expression extends AstNode
    permits BetweenExpression,
        BinaryExpression,
        FunctionCallExpression,
        IdentifierExpression,
        InExpression,
        LikeExpression,
        LiteralExpression,
        UnaryExpression {}
```

### 8.3 Node style

Each node should:

- use records unless mutation or lazy fields are required
- store only semantically relevant fields
- avoid retaining ANTLR context objects
- optionally carry a `SourceSpan`

### 8.4 Dialect-specific syntax

Use extension nodes when syntax differs enough to prevent clean normalization. Examples include:

- PostgreSQL `RETURNING`, array operators, type casts
- MySQL index hints and `ON DUPLICATE KEY`
- Oracle hierarchical queries and specialized clauses
- SQLite conflict resolution clauses

The AST model should avoid flattening these constructs into string payloads. They should remain typed nodes even when dialect-specific.

## 9. Error Model

The error model must be useful in applications and benchmarks.

### 9.1 Diagnostic shape

```java
public record SyntaxDiagnostic(
        DiagnosticSeverity severity,
        String message,
        int line,
        int column,
        String offendingToken
) {}
```

*(Future: `SourceSpan span` and `List<String> expectedTokens` may be added.)*

### 9.2 Error policy

- Fast path uses a bail strategy.
- Fallback path collects structured diagnostics.
- Parser contexts must not leak into public error types.
- The message should distinguish:
  - unsupported syntax in the current library scope
  - invalid syntax
  - ambiguous or partial parse conditions

## 10. Performance Strategy

Performance work starts in the first implementation milestone.

### 10.1 Primary tactics

1. Parse tree is built for AST mapping; consider disabling where optimization justifies it.
2. Use SLL-first with LL fallback.
3. Maintain dialect-specific entry points.
4. Avoid general-purpose intermediate models between ANTLR contexts and AST nodes.
5. Keep AST nodes compact and immutable.
6. Minimize string copies; prefer source-span references when practical.
7. Use specialized mapping code for hot constructs such as:
   - select lists
   - expression trees
   - joins
   - function calls

### 10.2 Secondary tactics

1. Tune lexer and parser setup per dialect.
2. Rework ambiguity-heavy grammar rules, especially in PostgreSQL.
3. Separate "strict" and "tolerant" parse modes if that materially improves the fast path.
4. Consider lightweight pooling only if benchmark data shows a gain.

### 10.3 Anti-goals

Avoid premature complexity such as:

- custom bytecode generation
- hand-written tokenizers
- unsafe memory techniques

Those options should be considered only if the benchmark data clearly justifies them.

## 11. Benchmark Plan

Benchmarking is required before claiming that the parser is faster than JSqlParser.

### 11.1 Harness

Use JMH with:

- dedicated benchmark module
- warmup and measurement iterations
- separate forks
- per-dialect benchmark groups

### 11.2 Corpus categories

Each dialect should include:

1. Small statements
   - simple `SELECT`
   - single-table DML
2. Medium statements
   - joins
   - subqueries
   - CTEs
   - window functions
3. Large statements
   - deep nesting
   - many projected expressions
   - large DDL blocks
4. Error-path statements
   - incomplete queries
   - invalid clause ordering
   - malformed expressions

### 11.3 Metrics

- operations per second
- average and percentile latency
- allocations per operation
- GC pressure
- success/failure mode overhead

### 11.4 Benchmark contract

The library may claim "faster than JSqlParser" only when:

1. the same SQL corpus is used,
2. both parsers run on the same JVM baseline,
3. equivalent work is measured as closely as possible, and
4. results are published with the benchmark configuration.

### 11.5 Performance milestones

- M1: establish baseline against JSqlParser with placeholder parser infrastructure
- M2: beat JSqlParser on MySQL single-statement throughput
- M3: beat JSqlParser on SQLite single-statement throughput
- M4: reach parity or better on PostgreSQL and Oracle SQL subsets

## 12. Dialect Rollout Strategy

### 12.1 Phase order

1. MySQL
2. SQLite
3. PostgreSQL
4. Oracle

### 12.2 Rationale

- MySQL has the strongest starting grammar.
- SQLite is narrower and should validate the architecture quickly.
- PostgreSQL requires grammar cleanup and therefore carries higher risk.
- Oracle scope needs explicit control to avoid PL/SQL sprawl.

### 12.3 Oracle scope for v1

The default v1 recommendation is:

- support Oracle SQL statement parsing
- do not promise full PL/SQL support

If procedural support is later required, add it as a separate milestone with its own API and benchmark expectations.

## 13. Testing and Conformance

### 13.1 Test layers

1. Grammar generation tests
2. Parser smoke tests
3. AST golden tests
4. corpus conformance tests
5. malformed input tests
6. differential tests where practical

### 13.2 Corpus sources

Test corpora should come from:

- vendor documentation examples
- public SQL examples
- internal regression files added during bug fixes

### 13.3 Golden tests

Golden tests should snapshot either:

- normalized AST JSON, or
- a canonical tree rendering

This makes regressions easier to detect than asserting large object graphs manually.

### 13.4 Differential testing

Where syntax overlaps, compare:

- parse success/failure
- structural equivalence of supported constructs
- stability of error positions

between `sqool` and selected reference parsers or vendor parsers.

## 14. Build and Code Generation Strategy

### 14.1 Build tooling

The project uses **Gradle** as the standard build tool with reproducible code generation. This choice aligns with the repository bootstrap and satisfies the key selection criteria:

- clean ANTLR integration
- multi-module ergonomics
- JMH integration
- fast CI caching

### 14.2 Generated source policy

Prefer generated sources to be reproducible from committed grammar files rather than edited by hand. Generated code may be:

- committed for easier consumer builds, or
- excluded and produced during build

This should be decided once CI and release workflow are in place. The recommended default is:

- commit grammar files
- generate sources during build

### 14.3 Version pinning

Pin:

- ANTLR tool version
- ANTLR runtime version
- JMH version
- Java release target

Do not float critical parser toolchain versions.

## 15. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
| --- | --- | --- | --- |
| PostgreSQL grammar cleanup is larger than expected | High | High | Isolate it in its own module and milestone. |
| Oracle grammar scope explodes into PL/SQL support | High | Medium | Limit v1 to Oracle SQL unless explicitly widened. |
| ANTLR parser underperforms JSqlParser on some workloads | High | Medium | Benchmark early, optimize fast path, and refine grammar ambiguity. |
| AST becomes too generic and loses dialect fidelity | Medium | Medium | Use dialect extension nodes instead of string-based escape hatches. |
| Public API leaks ANTLR details | Medium | Low | Keep all parser contexts internal to dialect modules. |
| Grammar drift from upstream | Medium | Medium | Record upstream commit hashes and local patches per dialect. |

## 16. Open Decisions

The following decisions should be finalized before dialect implementation expands beyond the bootstrap baseline:
1. Oracle v1 scope: SQL only or partial PL/SQL
2. Whether comment preservation (`collectComments`) is required in v1
3. Whether `ErrorTolerance` is needed in `ParseOptions`

Resolved:
- Generated ANTLR sources: produced during build, not committed
- Script parsing: implemented; supported for MySQL, SQLite, and PostgreSQL

## 17. Implementation Milestones

### Milestone 0: Project skeleton ✓

- create multi-module build
- add Java 25 baseline
- add ANTLR integration
- add core API and empty dialect stubs
- add JMH harness

### Milestone 1: MySQL MVP ✓

- vendor MySQL grammar
- generate parser
- implement SLL-first parse flow
- map a supported subset to normalized AST
- add first JSqlParser benchmarks

### Milestone 2: SQLite MVP ✓

- vendor SQLite grammar
- harden grammar with regression tests
- implement AST mapping
- benchmark and optimize against JSqlParser

### Milestone 3: PostgreSQL grammar hardening ✓

- vendor grammar
- clean up ambiguity and non-idiomatic constructs
- implement AST mapping for the common subset
- expand conformance corpus

### Milestone 4: Oracle SQL MVP

- vendor Oracle grammar base
- subset to SQL-first support
- implement AST mapping
- benchmark the supported subset

### Milestone 5: Cross-dialect stabilization

- unify diagnostics behavior
- expand AST coverage
- improve documentation
- publish benchmark reports in CI

## 18. Recommended Immediate Next Step

Milestones 0–3 are complete. The next steps are:

1. **Milestone 4: Oracle SQL MVP** — vendor Oracle grammar, subset to SQL-first, implement AST mapping and benchmarks
2. **Milestone 5: Cross-dialect stabilization** — unify diagnostics, expand AST coverage, publish benchmark reports
3. **Optional enhancements** — add `collectComments` and `ErrorTolerance` to `ParseOptions` if needed; extend `SyntaxDiagnostic` with `SourceSpan` and `expectedTokens`
