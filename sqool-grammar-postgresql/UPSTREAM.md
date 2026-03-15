# Upstream PostgreSQL Grammar

- Source repository: `https://github.com/antlr/grammars-v4`
- Upstream path: `sql/postgresql`
- Vendored from commit: `c81a1c36b8dcef78e1f3c24dad83a4a3f5ef6e8e` (December 2023)
- Original author: Dmitry Gaiduk, with contributions from the grammars-v4 community

## Vendored files

- `src/main/antlr/PostgreSQLLexer.g4`
- `src/main/antlr/PostgreSQLParser.g4`

## Local deviations from upstream

1. **Package header added**: Both lexer and parser have
   `@header { package io.github.e4c5.sqool.grammar.postgresql.generated; }` so that
   generated sources live in `io.github.e4c5.sqool.grammar.postgresql.generated`.

2. **Scoped to the v1 subset**: The upstream grammar covers the full PostgreSQL syntax.
   This fork is intentionally scoped to the core DML/DDL subset defined for Milestone 3
   (SELECT, INSERT, UPDATE, DELETE, CREATE TABLE, DROP TABLE, TRUNCATE, transaction
   control). This reduces grammar size and ambiguity surface.

3. **Expression grammar simplified**: The upstream grammar uses a complex, fully
   recursive expression model. This fork uses ANTLR4's left-recursive operator
   precedence convention with clearly ordered alternatives.

4. **Dollar-quoted string support simplified**: The upstream grammar uses a lexer
   predicate to handle tagged dollar-quote labels (e.g., `$tag$...$tag$`). This fork
   only supports the plain `$$...$$` form. Tagged dollar quotes are lexed as IDENTIFIER
   followed by a parsing error, which falls through to the raw-statement path.

5. **RETURNING clause as first-class rule**: The upstream grammar treats RETURNING as
   part of the statement options. Here it is a dedicated `returningClause` rule to make
   PostgreSQL-specific AST mapping straightforward.

6. **Generated sources excluded from Checkstyle and Javadoc**: Applied in
   `build.gradle.kts` to suppress quality-tool warnings on generated code.

## Known upstream issues

| Issue | Impact | Status in this fork |
|-------|--------|---------------------|
| Grammar covers the full PostgreSQL syntax (large) | High prediction cost for SLL mode on complex SQL | Mitigated by scoping to v1 subset |
| Some ambiguity in expression rules (e.g., type cast vs comparison) | Occasional LL fallback for borderline SQL | Accepted; tested via regression corpus |
| Dollar-quote with tags requires a stateful lexer predicate | Unusual in application SQL | Deferred; plain `$$` supported only |
| `INTERVAL` type field qualifiers are complex | Rarely used in application SQL | Simplified implementation included |

## Goals for this fork

- Provide a stable, project-controlled grammar for the PostgreSQL v1 subset.
- Minimize grammar ambiguity to keep SLL mode effective for typical application SQL.
- Serve as the foundation for future PostgreSQL grammar expansion (v2+).
- Remain easy to update against upstream when new PostgreSQL syntax is needed.
