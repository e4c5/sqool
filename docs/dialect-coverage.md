# Dialect Coverage Matrix

This document summarizes the SQL constructs supported by each sqool dialect parser as of Milestone 5.

## Legend

| Symbol | Meaning |
|--------|---------|
| ✅ Normalized | Parsed and mapped to a normalized AST node (e.g. `SelectStatement`). |
| ⚠ Raw | Parsed successfully and wrapped in a raw statement node (e.g. `MySqlRawStatement`). Parse succeeds; AST details are not normalized. |
| ❌ Unsupported | Not supported by the grammar or parser; parse fails with diagnostics. |
| — | Not applicable or syntax does not exist in the dialect. |

## Statement coverage

| Construct | MySQL | SQLite | PostgreSQL | Oracle |
|-----------|-------|--------|------------|--------|
| Simple SELECT (no FROM) | ✅ | ✅ | ✅ | ✅ (via DUAL) |
| SELECT with FROM (single table) | ✅ | ✅ | ✅ | ✅ |
| SELECT with WHERE | ✅ | ✅† | ✅ | ✅ |
| SELECT with ORDER BY | ✅ | ✅† | ✅ | ✅ |
| SELECT with GROUP BY / HAVING | ✅ | ✅† | ✅ | ✅ |
| SELECT with LIMIT / OFFSET | ✅ | ✅ | ✅ | — |
| SELECT with FETCH FIRST (Oracle) | — | — | — | ⚠ Raw |
| SELECT with INNER JOIN | ✅ | ⚠ Raw† | ✅ | ✅ |
| SELECT with LEFT / RIGHT JOIN | ✅ | ⚠ Raw† | ✅ | ✅ |
| SELECT with FULL OUTER JOIN | — | — | ✅ | ✅ |
| SELECT with CROSS JOIN | ✅ | ⚠ Raw† | ✅ | ✅ |
| SELECT with NATURAL JOIN | ⚠ Raw | ⚠ Raw† | ⚠ Raw | ⚠ Raw |
| SELECT DISTINCT | ✅ | ✅ | ✅ | ✅ |
| UNION / UNION ALL / INTERSECT | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| MINUS (Oracle) | — | — | — | ⚠ Raw |
| Subqueries in FROM (derived tables) | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| CTEs (WITH clause) | ⚠ Raw | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| INSERT … VALUES | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| INSERT … SELECT | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| INSERT … RETURNING (PostgreSQL) | — | — | ⚠ Raw | — |
| REPLACE INTO (MySQL) | ✅ | — | — | — |
| UPDATE | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| UPDATE multi-table (MySQL) | ✅ | — | — | — |
| DELETE | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| CREATE TABLE | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| CREATE TABLE … LIKE (MySQL) | ✅ | — | — | — |
| CREATE TABLE … IF NOT EXISTS | ✅ | ⚠ Raw | ⚠ Raw | — |
| DROP TABLE | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| DROP TABLE IF EXISTS | ✅ | ⚠ Raw | ⚠ Raw | — |
| TRUNCATE TABLE | ✅ | — | ⚠ Raw | ⚠ Raw |
| CREATE DATABASE | ✅ | — | — | — |
| DROP DATABASE | ✅ | — | — | — |
| SHOW statements (MySQL) | ✅ | — | — | — |
| Transaction control (BEGIN/COMMIT/ROLLBACK) | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| SAVEPOINT | ⚠ Raw | ⚠ Raw | ⚠ Raw | ⚠ Raw |

† SQLite's v1 expression mapper supports only simple expressions (literals, identifiers). Queries with binary comparisons in WHERE, join conditions in ON, or other complex expressions may fall back to `SqliteRawStatement`. See [Known Limitations](#known-limitations).

## Expression coverage

| Expression | MySQL | SQLite | PostgreSQL | Oracle |
|------------|-------|--------|------------|--------|
| Column/table identifier | ✅ | ✅ | ✅ | ✅ |
| String / number / NULL literals | ✅ | ✅ | ✅ | ✅ |
| Arithmetic (`+`, `-`, `*`, `/`) | ✅ | ✅ | ✅ | ✅ |
| Comparison (`=`, `<>`, `<`, `>`, etc.) | ✅ | ⚠ Limited | ✅ | ✅ |
| Logical AND / OR | ✅ | ✅ | ✅ | ✅ |
| NOT | ✅ | ✅ | ✅ | ✅ |
| IS NULL / IS NOT NULL | ✅ | ✅ | ✅ | ✅ |
| BETWEEN | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| IN | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| LIKE | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| Function calls | ✅ | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| Subquery expressions | ⚠ Raw | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| CASE expressions | ⚠ Raw | ⚠ Raw | ⚠ Raw | ⚠ Raw |
| CONCAT `\|\|` (Oracle) | — | — | — | ⚠ Raw |

## Normalized AST node mapping

| AST Node | MySQL | SQLite | PostgreSQL | Oracle |
|----------|-------|--------|------------|--------|
| `SelectStatement` | ✅ | ✅† | ✅ | ✅ |
| `InsertStatement` | ✅ | — | — | — |
| `UpdateStatement` | ✅ | — | — | — |
| `DeleteStatement` | ✅ | — | — | — |
| `CreateTableStatement` | ✅ | — | — | — |
| `DropTableStatement` | ✅ | — | — | — |
| `CreateDatabaseStatement` | ✅ | — | — | — |
| `DropDatabaseStatement` | ✅ | — | — | — |
| `TruncateTableStatement` | ✅ | — | — | — |
| `ReplaceStatement` | ✅ | — | — | — |
| `ShowStatement` | ✅ | — | — | — |
| `SetOperationStatement` | ✅ | — | — | — |
| `NamedTableReference` | ✅ | ✅ | ✅ | ✅ |
| `JoinTableReference` | ✅ | — | ✅ | ✅ |
| `DerivedTableReference` | ✅ | — | — | — |
| `MySqlRawStatement` | ✅ | — | — | — |
| `SqliteRawStatement` | — | ✅ | — | — |
| `PostgresqlRawStatement` | — | — | ✅ | — |
| `OracleRawStatement` | — | — | — | ✅ |

† SQLite `SelectStatement` is produced for queries where the expression mapper can handle all expressions (no binary comparisons in WHERE, no explicit joins).

## Dialect-specific features

### MySQL
- `REPLACE INTO` (upsert semantics)
- `SHOW` statements
- Multi-table `UPDATE`
- `INSERT … ON DUPLICATE KEY UPDATE`
- Backtick-quoted identifiers
- Script mode via `queries()` entry point

### SQLite
- Loose typing with `INTEGER`, `TEXT`, `REAL`, `BLOB`, `NUMERIC`
- `PRAGMA` statements
- Attach/Detach databases
- Virtual tables
- Script mode via `sql_stmt_list` entry point

### PostgreSQL
- `INSERT … RETURNING`
- `FETCH FIRST n ROWS ONLY` (as raw)
- `OFFSET n` clause
- `$$`-quoted strings (grammar-level)
- Script mode via `root()` entry point

### Oracle
- `NUMBER`, `VARCHAR2`, `NVARCHAR2`, `CLOB`, `BLOB` column types
- `DUAL` table for scalar expressions
- `ROWNUM`, `ROWID`, `SYSDATE` pseudo-columns (as literals/identifiers)
- `FETCH FIRST n ROWS ONLY` (as raw)
- `MINUS` set operator (as raw, no SQLite/MySQL equivalent)
- No PL/SQL (anonymous blocks, stored procedures, triggers) – explicitly out of scope for v1
- Script mode via `root()` entry point

## Known limitations

### Common
- CTEs (`WITH` clause) are parsed but fall back to raw statements in all dialects.
- Subqueries in `FROM` are normalized only in MySQL (`DerivedTableReference`); other dialects use raw.
- `NATURAL JOIN` is parsed but falls back to raw in all dialects (not normalized to `JoinTableReference`).
- Function calls in SELECT / WHERE are parsed but fall back to raw in all dialects except MySQL.

### SQLite
- The v1 expression mapper navigates a strict single-path through the expression grammar. Queries with comparison operators in WHERE (e.g. `WHERE id = 1`), join ON conditions, or other expressions not at the terminal level fall back to `SqliteRawStatement`. This is a known v1 scope limitation; the parse always succeeds, but normalization is limited.
- `JOIN` queries fall back to `SqliteRawStatement` because `JoinTableReference` is not yet implemented for SQLite.

### PostgreSQL
- `OFFSET … FETCH FIRST` (SQL standard paging syntax) is parsed but returned as `PostgresqlRawStatement`.
- Subqueries in FROM fall back to raw.

### Oracle
- No PL/SQL procedural code (anonymous blocks, stored procedures, package declarations, triggers).
- `FETCH FIRST n ROWS ONLY` is parsed but returned as `OracleRawStatement`.
- Subqueries in FROM fall back to raw.

## Usage examples

```java
// MySQL
SqlParser mysqlParser = new MysqlSqlParser();
ParseResult result = mysqlParser.parse(
    "SELECT u.id, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id",
    ParseOptions.defaults(SqlDialect.MYSQL));

// SQLite
SqlParser sqliteParser = new SqliteSqlParser();
ParseResult result = sqliteParser.parse(
    "SELECT id, name FROM users",
    ParseOptions.defaults(SqlDialect.SQLITE));

// PostgreSQL
SqlParser pgParser = new PostgresqlSqlParser();
ParseResult result = pgParser.parse(
    "SELECT u.id, o.total FROM users u LEFT JOIN orders o ON u.id = o.user_id",
    ParseOptions.defaults(SqlDialect.POSTGRESQL));

// Oracle
SqlParser oracleParser = new OracleSqlParser();
ParseResult result = oracleParser.parse(
    "SELECT e.id, d.name FROM employees e INNER JOIN departments d ON e.dept_id = d.id",
    ParseOptions.defaults(SqlDialect.ORACLE));

// Script mode (multiple statements)
ParseResult script = mysqlParser.parse(
    "INSERT INTO users (id) VALUES (1); SELECT * FROM users;",
    ParseOptions.defaults(SqlDialect.MYSQL).withScriptMode(true));

// Inspect the result
switch (result) {
    case ParseSuccess success -> {
        AstNode root = success.root();
        if (root instanceof SelectStatement sel) {
            // Normalized SELECT
        } else if (root instanceof MySqlRawStatement raw) {
            // Fallback raw statement
        }
    }
    case ParseFailure failure -> {
        for (SyntaxDiagnostic d : failure.diagnostics()) {
            System.err.printf("Error at %d:%d – %s%n", d.line(), d.column(), d.message());
        }
    }
}
```
