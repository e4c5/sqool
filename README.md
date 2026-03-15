# sqool

ANTLR-based SQL parser for Java 25 targeting MySQL, PostgreSQL, Oracle, and SQLite.

## Build baseline

- Build tool: Gradle
- Target runtime: Java 25
- Current status: Milestone 5 (cross-dialect stabilization) complete; all four dialects (MySQL, SQLite, PostgreSQL, Oracle) have stable parser implementations with cross-dialect conformance tests.

## Current parser coverage

- **MySQL**
  - Real upstream grammar vendored from `antlr/grammars-v4` (see `sqool-grammar-mysql/UPSTREAM.md`).
  - Parser facade with SLL-first parsing and LL fallback; parser metrics via `ParseMetrics`.
  - Normalized AST: `CREATE TABLE`, `CREATE DATABASE`, `DELETE`, `DROP TABLE`/`DROP DATABASE`, derived tables, `GROUP BY`, `HAVING`, `INSERT`/`REPLACE`, `SELECT` with joins/aliases/expressions, `SHOW` statements, `TRUNCATE`, `UNION`/`UNION ALL`, `UPDATE`, script mode, and more. Unsupported statement kinds fall back to `MySqlRawStatement`.
  - Conformance and regression suite in `sqool-conformance`; JMH benchmark in `sqool-bench` (vs JSqlParser).
- **SQLite**
  - Grammar vendored in `sqool-grammar-sqlite` (see `UPSTREAM.md`). Parser in `sqool-dialect-sqlite` with SLL/LL and `ParseMetrics`.
  - Normalized AST for core SELECT (simple queries without complex expressions), and raw-statement fallback for all other constructs. Cross-dialect conformance tests alongside MySQL, PostgreSQL, and Oracle. JMH benchmark in `sqool-bench`.
- **PostgreSQL**
  - Grammar vendored in `sqool-grammar-postgresql` (see `UPSTREAM.md`). Parser in `sqool-dialect-postgresql` with SLL/LL and `ParseMetrics`.
  - Normalized AST: `SELECT` (including single-table and JOIN queries), with raw-statement fallback for INSERT, UPDATE, DELETE, CREATE TABLE, DROP TABLE, and transaction control. Conformance and JMH benchmark present.
- **Oracle**
  - SQL-only v1 subset: SELECT (including JOINs), INSERT, UPDATE, DELETE, CREATE TABLE, DROP TABLE, TRUNCATE, transaction control (COMMIT, ROLLBACK, SAVEPOINT). PL/SQL procedural constructs (anonymous blocks, stored procedures, triggers) are explicitly out of scope for v1.
  - Grammar written for Oracle SQL v1 in `sqool-grammar-oracle`. Uses Oracle-specific types (`NUMBER`, `VARCHAR2`, `NVARCHAR2`, `CLOB`, `BLOB`, etc.) and constructs (`ROWNUM`, `ROWID`, `DUAL`, `SYSDATE`, `FETCH FIRST n ROWS ONLY`, `MINUS` set operator).
  - Parser in `sqool-dialect-oracle` with SLL/LL fallback pattern and `ParseMetrics`. Normalized AST: SELECT (single-table and JOIN queries); all other statements fall back to `OracleRawStatement`. Conformance suite in `sqool-conformance` and JMH benchmark in `sqool-bench`.
  - See [Oracle Risk and Scope](docs/oracle-risk-and-scope.md) for scope limitations and risk mitigation details.

See [Dialect Coverage Matrix](docs/dialect-coverage.md) for a full breakdown of supported constructs and normalization level per dialect.

## Common commands

```bash
./gradlew build
./gradlew verifyBootstrap
./gradlew :sqool-bench:jmh
```

See [Benchmarks](docs/benchmarks.md) for how to run and capture baseline results.

## Design documents

- [SQL Parser Technical Design](docs/sql-parser-technical-design.md)
- [SQL Parser High-Level Implementation Checklist](docs/sql-parser-implementation-checklist.md)
- [Dialect Coverage Matrix](docs/dialect-coverage.md)
- [Diagnostics Behavior](docs/diagnostics.md)
- [Benchmarks](docs/benchmarks.md)
- [Milestone 0 Bootstrap Backlog](docs/milestone-0-bootstrap-backlog.md)
- [Milestone 0 Review Checklist](docs/milestone-0-review-checklist.md)
- [Milestone 4 Oracle Backlog](docs/milestone-4-oracle-backlog.md)
- [Milestone 4 Review Checklist](docs/milestone-4-review-checklist.md)
- [Oracle Risk and Scope](docs/oracle-risk-and-scope.md)
- [Milestone 5 Cross-Dialect Backlog](docs/milestone-5-cross-dialect-backlog.md)
- [Milestone 5 Review Checklist](docs/milestone-5-review-checklist.md)
