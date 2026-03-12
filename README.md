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
    - `DISTINCT`
    - aliases
    - arithmetic expressions
    - aggregate and generic function calls
    - selected runtime built-in functions (`COALESCE`, `IF`, `MOD`, `DATE`, `NOW`, `CURDATE`, `CURRENT_USER`)
    - derived tables
    - `GROUP BY`
    - `HAVING`
    - `IN` / `BETWEEN` / `LIKE`
    - qualified references
    - script mode for multi-statement SELECT batches
    - `UNION` / `UNION ALL`
    - joins with `USING`
    - `WHERE`
    - explicit joins with `ON`
    - `ORDER BY`
    - numeric `LIMIT`
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
