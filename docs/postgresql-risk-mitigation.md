# PostgreSQL Risk and Mitigation – Milestone 3

## 1. Purpose

This document maps the PostgreSQL-specific risks identified in the technical design
to the actual work completed in Milestone 3, and records remaining risks going into
subsequent milestones.

Reference: `sql-parser-technical-design.md` (§12 Dialect rollout, §17 Milestones,
PostgreSQL risks section).

## 2. Risks mitigated in Milestone 3

| Risk | Mitigation in M3 |
|------|-----------------|
| **Grammar size and complexity** – The upstream grammars-v4 PostgreSQL grammar covers the full language and is very large. | Forked grammar scoped to the v1 subset: only DML/DDL + SELECT + transaction control. Grammar compiles cleanly and SLL mode is effective for typical application SQL. |
| **Ambiguity in expression rules** – SQL expression grammars often have hidden ambiguities that cause prediction failures and expensive LL fallback. | Expression rule uses ANTLR4's precedence-climbing left-recursive model with clearly ordered alternatives. BETWEEN…AND conflict resolved by precedence ordering. |
| **Dollar-quoted string literals** – Tagged dollar quotes require a stateful lexer predicate, which complicates grammar maintenance. | Plain `$$...$$` supported. Tagged form deferred; falls through to raw-statement path without parser failure. |
| **Keyword-identifier conflicts** – PostgreSQL has many unreserved keywords that are valid identifiers, which can cause parse failures in typical queries. | `unreservedKeyword` rule enumerates safe keyword-as-identifier cases; `name` rule covers IDENTIFIER, QUOTED_IDENTIFIER, and unreservedKeyword. |
| **RETURNING clause** – PostgreSQL-specific feature not present in MySQL or SQLite grammars; needs first-class treatment for the PostgreSQL extension node story. | `returningClause` is a first-class grammar rule. Statements with RETURNING are parsed correctly and fall back to `PostgresqlRawStatement` (since full AST mapping is deferred to M4). |
| **Parser pipeline not yet wired** – No PostgreSQL dialect existed before M3. | `PostgresqlSqlParser` implements the shared `SqlParser` contract; SLL-first with LL fallback; wired to `SqlDialect.POSTGRESQL` (already defined in core). |
| **No AST mapping for PostgreSQL** | `PostgresqlAstMapper` maps simple SELECT statements to `SelectStatement`; all other statements map to `PostgresqlRawStatement` with correct `PostgresqlStatementKind`. |
| **No test coverage for PostgreSQL** | Grammar smoke tests (10), dialect unit tests (23), conformance tests (11 supported + 2 failure), benchmarks (PostgresqlParserBenchmark). |
| **No benchmark baseline** | `PostgresqlParserBenchmark` added to `sqool-bench`; measures throughput for join queries, simple queries, and error path vs JSqlParser baseline. |

## 3. Remaining risks going into M4+

| Risk | Priority | Notes |
|------|----------|-------|
| **Join AST mapping** – Queries with joins fall back to `PostgresqlRawStatement`. | High | Target: M4. Requires `JoinTableReference` mapping in the PostgreSQL mapper. |
| **Complex SELECT mapping** – CTEs, set operations (UNION/INTERSECT/EXCEPT), window functions, and subqueries in FROM all fall back to raw. | Medium | Target: M4. Map common patterns progressively. |
| **INSERT/UPDATE/DELETE AST mapping** – Currently all DML (except SELECT) falls back to raw. | Medium | Target: M4. Enable normalized `InsertStatement`, `UpdateStatement`, `DeleteStatement` mapping with RETURNING as an extension node. |
| **`PostgresqlRawStatement` extension nodes** – RETURNING and other PostgreSQL-specific features are not yet modeled as normalized extension nodes. | Medium | Target: M4. Add `ReturningClause` to the AST and model it alongside the statement. |
| **Tagged dollar-quote support** – `$tag$...$tag$` strings tokenize incorrectly. | Low | Impact is limited to PL/pgSQL function bodies, which are outside the DML/DDL subset. Defer until function/procedure support is targeted. |
| **`unreservedKeyword` coverage gaps** – Some keyword-identifier combinations may not be in the list, causing unnecessary LL fallback or raw fallback. | Low | Address incrementally as new queries are added to the conformance corpus. |
| **Grammar version alignment** – The vendored grammar is based on PostgreSQL 15 syntax. PostgreSQL 16/17 may introduce new constructs. | Low | Normal maintenance. Update grammar and conformance corpus when new PostgreSQL versions add syntax to the v1 subset. |

## 4. Deliberate scope limitations for the v1 subset

The following PostgreSQL features are explicitly out of scope for M3 and are documented
here to avoid re-discovery:

1. **PL/pgSQL** – Procedural language blocks (`DO $$ BEGIN ... END $$;`) are not in scope.
2. **COPY** – Bulk data import/export is not in the DML v1 subset.
3. **VACUUM / ANALYZE** – Maintenance commands are not in scope.
4. **ALTER TABLE** – Schema change commands are not in the v1 subset.
5. **CREATE INDEX / CREATE VIEW / CREATE FUNCTION** – Extended DDL deferred to a later milestone.
6. **Window functions** – Recognized by the grammar but not mapped to the normalized AST.
7. **Full-text search operators** – `@@ `, `@>`, `<@` and similar PostgreSQL-specific operators are recognized as tokens but mapping is not implemented.
8. **JSON operators** – `->`, `->>`, `#>`, `#>>` are lexed but not mapped.

## 5. Summary

Milestone 3 has established a stable foundation for PostgreSQL parsing:

- The grammar is vendored, documented, and under project control.
- A working parser pipeline is in place behind the public API.
- Simple SELECT mapping to the normalized AST works for the core v1 subset.
- All other statement types produce useful raw-statement results (no silent data loss).
- Conformance and regression coverage is in place and extendable.
- Benchmark baselines are recorded.

The primary remaining work is expanding the AST mapping depth (joins, DML, extension nodes)
in M4, which can proceed without structural changes to the grammar or parser infrastructure.
