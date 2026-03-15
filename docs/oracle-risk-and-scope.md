# Oracle SQL Parser – Risk and Scope

## 1. Scope

This document describes the risk profile and scope limitations for the Oracle SQL parser
(sqool Milestone 4). The Oracle dialect is designed as **SQL-only v1**: it parses standard
Oracle SQL statements and is intentionally out of scope for PL/SQL procedural language constructs.

### In-scope Oracle SQL v1 subset

| Category | Supported constructs |
|----------|---------------------|
| SELECT | `SELECT [DISTINCT] ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ...` |
| Row limiting | `FETCH FIRST n ROWS ONLY` (Oracle 12c+), `OFFSET ... FETCH FIRST ...` (raw fallback) |
| Joins | `INNER`, `LEFT`, `RIGHT`, `FULL [OUTER]`, `CROSS`, `NATURAL` (raw fallback) |
| INSERT | `INSERT INTO ... VALUES (...)`, `INSERT INTO ... SELECT ...` |
| UPDATE | `UPDATE ... SET ... WHERE ...` |
| DELETE | `DELETE [FROM] ... WHERE ...` |
| CREATE TABLE | Standard column definitions with Oracle types, constraints |
| DROP TABLE | `DROP TABLE [IF EXISTS] ...`, `CASCADE CONSTRAINTS`, `PURGE` |
| TRUNCATE | `TRUNCATE [TABLE] ...` |
| Transactions | `COMMIT`, `ROLLBACK`, `SAVEPOINT`, `ROLLBACK TO [SAVEPOINT] ...` |
| Types | `NUMBER`, `VARCHAR2`, `NVARCHAR2`, `CHAR`, `NCHAR`, `DATE`, `TIMESTAMP`, `CLOB`, `BLOB`, `XMLTYPE`, `BINARY_FLOAT`, `BINARY_DOUBLE`, etc. |
| Oracle pseudo-columns | `ROWNUM`, `ROWID` (lexed and parsed; available as identifiers) |
| Oracle built-ins | `DUAL` table, `SYSDATE`, `SYSTIMESTAMP` literals |
| Set operators | `UNION [ALL]`, `INTERSECT`, `MINUS` (raw fallback) |

### Explicitly out-of-scope for v1

| Category | Exclusion rationale |
|----------|---------------------|
| Anonymous PL/SQL blocks | `BEGIN ... END` blocks are PL/SQL procedural constructs; excluded by design. The `BEGIN` keyword is not a statement entry point. |
| Stored procedures and functions | PL/SQL-only; not needed for SQL statement parsing. |
| Triggers | PL/SQL-only. |
| `CONNECT BY` / `START WITH` | Hierarchical queries; complex and rarely needed in application SQL. Deferred to v2. |
| `MERGE` statement | Complex upsert syntax; deferred to v2. |
| `RETURNING INTO` | PL/SQL variable binding; deferred to v2. |
| `BULK COLLECT` | PL/SQL-only. |
| `CURSOR` declarations | PL/SQL-only. |
| Type casts (`CAST(...)`) | Can be added as expression form in v2. |
| `PIVOT` / `UNPIVOT` | Complex analytical SQL; deferred to v2. |
| Table partitioning clauses | DDL extension; deferred to v2. |
| Oracle hints (`/*+ HINT */`) | Optimizer hints; deferred to v2. |

## 2. Risks mitigated

### R1: PL/SQL sprawl
**Risk**: The upstream grammars-v4 Oracle grammar covers the full PL/SQL language, making it
very large and introducing high prediction costs for SLL mode.

**Mitigation**: The sqool Oracle grammar was written from scratch as a SQL-first subset. The
`statement` rule only contains SQL statement types; anonymous PL/SQL blocks are excluded from
the entry points. The SQL/PL/SQL boundary is documented in `UPSTREAM.md`.

### R2: Oracle keyword conflicts with identifiers
**Risk**: Oracle has a large number of keywords that are commonly used as column or table names
in application SQL (e.g., `level`, `date`, `start`).

**Mitigation**: The grammar uses an `unreservedKeyword` rule that allows commonly used keywords
as identifiers. The `name` rule accepts `IDENTIFIER`, `QUOTED_IDENTIFIER`, or `unreservedKeyword`.
Quoted identifiers (double-quoted) always work.

### R3: Grammar ambiguity in SLL mode
**Risk**: Oracle's SQL grammar has some inherent ambiguity that can cause SLL parsing failures,
forcing LL fallback for every statement.

**Mitigation**: The grammar is designed to minimize ambiguity in common SQL patterns. SLL mode is
tried first with `BailErrorStrategy`; LL mode is used as fallback when SLL fails. The
`ParseMetrics` object records which mode was used for observability.

### R4: Oracle type system complexity
**Risk**: Oracle's type system is complex, with many subtypes and vendor-specific forms.

**Mitigation**: The grammar covers the most common Oracle types. Unusual type forms that fail to
parse will cause the statement to fail rather than silently misparse.

## 3. Remaining risks

### R5: Undocumented Oracle SQL variants
Oracle SQL has many legacy and undocumented constructs that may appear in production databases.
These will produce parse failures (returning `ParseFailure` with diagnostics). This is acceptable
for v1.

### R6: Character set and NLS variations
Oracle supports national character set strings (`N'...'`), which are not included in the v1
grammar. These will produce parse failures.

### R7: Oracle-specific functions
Oracle has hundreds of built-in functions (e.g., `NVL`, `DECODE`, `TO_DATE`, `REGEXP_LIKE`).
These are supported as generic `functionCall` expressions and will parse correctly. However,
they will fall through to the raw statement fallback for AST mapping.

## 4. Future work (v2+)

- Add `MERGE` statement support.
- Add `CONNECT BY` / `START WITH` hierarchical query support.
- Add `RETURNING INTO` clause for DML.
- Add `PIVOT` and `UNPIVOT` support.
- Add Oracle optimizer hints as a lexer-level construct.
- Normalize JOIN queries into the shared AST.
- Add `CAST(expr AS type)` expression support.
