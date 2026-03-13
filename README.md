# sqool

ANTLR-based SQL parser for Java 25 targeting MySQL, PostgreSQL, Oracle, and SQLite.

## Build baseline

- Build tool: Gradle
- Target runtime: Java 25
- Current status: Milestone 0 baseline implemented, MySQL MVP slice implemented

## Current parser coverage

- MySQL:
  - real upstream grammar vendored from `antlr/grammars-v4`
  - parser facade implemented with SLL-first parsing and LL fallback
  - normalized AST currently supports:
    - `CREATE TABLE`
    - `CREATE DATABASE`
    - `DISTINCT`
    - `DELETE`
    - `DROP TABLE`
    - derived tables
    - `GROUP BY`
    - `HAVING`
    - `IN` / `BETWEEN` / `LIKE`
    - `INSERT`
    - aliases
    - arithmetic expressions
    - aggregate and generic function calls
    - selected runtime built-in functions (`COALESCE`, `IF`, `MOD`, `DATE`, `NOW`, `CURDATE`, `CURRENT_USER`)
    - qualified references
    - script mode for mixed multi-statement SELECT/DML/DDL batches
    - `SHOW COLUMNS`
    - `SHOW CREATE TABLE`
    - `SHOW DATABASES`
    - `SHOW TABLES`
    - `TRUNCATE TABLE`
    - `UNION` / `UNION ALL`
    - `UPDATE`
    - `REPLACE`
    - joins with `USING`
    - `WHERE`
    - explicit joins with `ON`
    - `ORDER BY`
    - numeric `LIMIT`
  - valid but not-yet-normalized MySQL statement kinds fall back to a typed `MySqlRawStatement` node
  - resource-backed conformance/regression suite for supported and unsupported MySQL corpus cases
  - JMH benchmark includes a first comparison against JSqlParser
- PostgreSQL, Oracle, SQLite:
  - planned, not yet implemented

## Common commands

```bash
./gradlew build
./gradlew verifyBootstrap
./gradlew :sqool-bench:jmh
```

## Design documents

- [SQL Parser Technical Design](docs/sql-parser-technical-design.md)
- [SQL Parser High-Level Implementation Checklist](docs/sql-parser-implementation-checklist.md)
- [Milestone 0 Bootstrap Backlog](docs/milestone-0-bootstrap-backlog.md)
- [Milestone 0 Review Checklist](docs/milestone-0-review-checklist.md)
