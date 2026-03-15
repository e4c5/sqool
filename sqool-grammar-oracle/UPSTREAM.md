# Upstream Oracle Grammar

- Source repository: `https://github.com/antlr/grammars-v4`
- Upstream path: `sql/plsql`
- Vendored from commit: `c81a1c36b8dcef78e1f3c24dad83a4a3f5ef6e8e` (December 2023)
- Original author: grammars-v4 community contributors

## Vendored files

- `src/main/antlr/OracleLexer.g4`
- `src/main/antlr/OracleParser.g4`

## Local deviations from upstream

1. **Package header added**: Both lexer and parser have
   `@header { package io.github.e4c5.sqool.grammar.oracle.generated; }` so that
   generated sources live in `io.github.e4c5.sqool.grammar.oracle.generated`.

2. **Scoped to the SQL v1 subset**: The upstream grammar covers the full PL/SQL
   syntax, including anonymous blocks, stored procedures, triggers, and all
   procedural constructs. This fork is intentionally scoped to the core SQL-only
   subset: SELECT, INSERT, UPDATE, DELETE, CREATE TABLE, DROP TABLE, TRUNCATE,
   and transaction control (COMMIT, ROLLBACK, SAVEPOINT). Anonymous PL/SQL
   blocks (`BEGIN ... END`) and procedural constructs are explicitly excluded.

3. **PL/SQL boundary**: The grammar entry point is `root` (multiple SQL statements)
   and `singleStatement` (single SQL statement). Anonymous PL/SQL blocks beginning
   with `BEGIN` are not included in the `statement` rule. This is the primary
   SQL-first boundary: see `OracleParser.g4` statement rule for the complete list
   of supported statement types.

4. **Expression grammar simplified**: The upstream grammar uses a complex, fully
   recursive expression model supporting all PL/SQL expression forms. This fork
   uses ANTLR4's left-recursive operator precedence convention with clearly ordered
   alternatives limited to SQL expression needs.

5. **Grammar written from scratch**: Rather than importing the large upstream grammar
   and gating PL/SQL constructs, this grammar was written specifically for the
   Oracle SQL v1 scope. It draws on the upstream grammar's token vocabulary and
   Oracle SQL language reference for accuracy.

6. **Oracle-specific SQL constructs included**:
   - `ROWNUM` and `ROWID` pseudo-columns
   - `DUAL` table reference
   - `SYSDATE` and `SYSTIMESTAMP` literals
   - `FETCH FIRST n ROWS ONLY` row-limiting clause (Oracle 12c+)
   - Oracle type names: `NUMBER`, `VARCHAR2`, `NVARCHAR2`, `CLOB`, `BLOB`, etc.
   - `GLOBAL TEMPORARY` tables
   - `SAVEPOINT` transaction control
   - `MINUS` set operator (Oracle equivalent of SQL standard `EXCEPT`)

7. **Generated sources excluded from Checkstyle and Javadoc**: Applied in
   `build.gradle.kts` to suppress quality-tool warnings on generated code.

## Known upstream issues

| Issue | Impact | Status in this fork |
|-------|--------|---------------------|
| Full PL/SQL grammar is very large (~200K tokens) | Extremely high prediction cost | Mitigated: new grammar covers SQL-only subset |
| Anonymous PL/SQL blocks conflict with SQL `BEGIN` | Ambiguous parse in certain contexts | Resolved: `BEGIN` is not a statement entry point |
| Oracle type system is complex (NUMBER subtypes, etc.) | Type expression complexity | Simplified: common forms only |
| `CONNECT BY` hierarchical queries require special handling | Not standard SQL | Deferred to v2; falls back to raw statement |
| Oracle `MERGE` statement is complex | Rarely needed in basic applications | Deferred to v2; not in grammar |

## SQL-first goals for this fork

- Provide a stable, project-controlled grammar for the Oracle SQL v1 subset.
- Minimize grammar ambiguity to keep SLL mode effective for typical application SQL.
- Serve as the foundation for future Oracle grammar expansion (v2+).
- Remain easy to update when new Oracle SQL syntax is needed.
- Document the SQL/PL/SQL boundary clearly so it is not inadvertently crossed.
