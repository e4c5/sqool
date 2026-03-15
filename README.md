# sqool

ANTLR-based SQL parser for Java 25 targeting MySQL, PostgreSQL, Oracle, and SQLite.

## Build baseline

- Build tool: Gradle
- Target runtime: Java 25
- Current status: Milestone 0 done; MySQL, SQLite, and PostgreSQL MVP slices implemented

## Current parser coverage

- **MySQL**
  - Real upstream grammar vendored from `antlr/grammars-v4` (see `sqool-grammar-mysql/UPSTREAM.md`).
  - Parser facade with SLL-first parsing and LL fallback; parser metrics via `ParseMetrics`.
  - Normalized AST: `CREATE TABLE`, `CREATE DATABASE`, `DELETE`, `DROP TABLE`/`DROP DATABASE`, derived tables, `GROUP BY`, `HAVING`, `INSERT`/`REPLACE`, `SELECT` with joins/aliases/expressions, `SHOW` statements, `TRUNCATE`, `UNION`/`UNION ALL`, `UPDATE`, script mode, and more. Unsupported statement kinds fall back to `MySqlRawStatement`.
  - Conformance and regression suite in `sqool-conformance`; JMH benchmark in `sqool-bench` (vs JSqlParser).
- **SQLite**
  - Grammar vendored in `sqool-grammar-sqlite` (see `UPSTREAM.md`). Parser in `sqool-dialect-sqlite` with SLL/LL and `ParseMetrics`.
  - Normalized AST for core SELECT, CREATE TABLE, INSERT, and raw-statement fallback. Conformance tests and JMH benchmark in `sqool-conformance` and `sqool-bench`. Cross-dialect tests (MySQL vs SQLite) in `sqool-conformance`.
- **PostgreSQL**
  - Grammar vendored in `sqool-grammar-postgresql` (see `UPSTREAM.md`). Parser in `sqool-dialect-postgresql` with SLL/LL and `ParseMetrics`. v1 subset: SELECT, INSERT, UPDATE, DELETE, CREATE TABLE, DROP TABLE, transaction control. Conformance and JMH benchmark present.
- **Oracle**
  - Planned, not yet implemented.

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
- [Benchmarks](docs/benchmarks.md)
- [Milestone 0 Bootstrap Backlog](docs/milestone-0-bootstrap-backlog.md)
- [Milestone 0 Review Checklist](docs/milestone-0-review-checklist.md)
- [Milestone 4 Oracle Backlog](docs/milestone-4-oracle-backlog.md)
- [Milestone 4 Review Checklist](docs/milestone-4-review-checklist.md)
- [Milestone 5 Cross-Dialect Backlog](docs/milestone-5-cross-dialect-backlog.md)
- [Milestone 5 Review Checklist](docs/milestone-5-review-checklist.md)
