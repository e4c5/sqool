# Parser and Mapper Abstractions

This document describes the shared parser and AST-mapping abstractions used across dialect modules. It is the design note for M2-1 (dialect generalization) and future refactors.

## Current architecture

- **No `AntlrParserFacade`**: Each dialect has its own parser facade class (e.g. `MysqlSqlParser`, `SqliteSqlParser`) that owns the parse flow. There is no shared "parser facade" base or interface beyond the public `SqlParser` API.
- **Shared pieces**: All dialects use the same **error listener** (`AntlrSyntaxErrorListener` in `sqool-core`), **source-span** helpers (`SourceSpans`), **parse metrics** (`ParseMetrics`), and **parse attempt** / **diagnostic** types. Lexer and parser instances are created per-dialect from each grammar’s generated classes.
- **Mapper pattern**: Each dialect has a package-private `*AstMapper` (e.g. `MysqlAstMapper`, `SqliteAstMapper`) that maps ANTLR parse-tree contexts to the normalized AST. Mappers do not share a common base class; they follow the same patterns (visit root → dispatch by statement type → map to `Statement` or raw wrapper).

## Shared components (sqool-core)

| Component | Purpose |
|-----------|---------|
| `AntlrSyntaxErrorListener` | Collects syntax errors during parse; produces `SyntaxDiagnostic` with line, column, message, offending token. Used by all four dialects. |
| `SourceSpans.fromTokens(...)` | Builds `SourceSpan` from ANTLR tokens when `ParseOptions` request source spans. |
| `ParseMetrics` | Records prediction mode (SLL vs LL) and elapsed time; attached to `ParseResult`. |
| `ParseOptions` | Dialect, script mode, fallback enabled, etc. Passed through the mapper. |
| `ParseAttempt` | Wraps parse context + diagnostics for SLL/LL attempt. |

## Shared AST (sqool-ast)

Normalized statement and expression types (`SelectStatement`, `InsertStatement`, `JoinTableReference`, `Expression`, etc.) live in `sqool-ast`. Dialect-specific fallbacks use `*RawStatement` wrappers (e.g. `MySqlRawStatement`, `SqliteRawStatement`). The public API exposes only `ParseResult` and AST node types; no ANTLR types leak.

## Dialect-specific vs shared

- **Dialect-specific**: Grammar files, generated lexer/parser, entry rule (e.g. `select_stmt` vs `query`), and any grammar-specific context types. Each dialect’s `*AstMapper` knows its grammar’s rule and context shapes.
- **Shared**: Error handling, diagnostics shape, source spans, metrics, normalized AST types, and the pattern “SLL first, LL fallback with diagnostics.”

## Possible future refactors

- **`AntlrParserFacade`**: A shared helper that takes a lexer/parser factory and runs the SLL-then-LL flow could reduce duplication in the four parser facades. Not required for current correctness; would be a clarity/maintainability refactor.
- **Shared mapping helpers**: Common logic for mapping expressions, literals, identifiers, or JOIN clauses could be moved into `sqool-core` or `sqool-ast` and reused by dialect mappers. Today, MySQL, SQLite, PostgreSQL, and Oracle each have their own mapping code; patterns are similar but not factored into a single abstraction.

## References

- [CONTRIBUTING.md](../CONTRIBUTING.md) – Shared helpers, naming, adding dialects.
- [sql-parser-technical-design.md](sql-parser-technical-design.md) – Overall architecture and milestones.
- [.cursor/rules/dialect-generalization.mdc](../.cursor/rules/dialect-generalization.mdc) – Rule: generalize first, then reuse; do not copy from MySQL.
