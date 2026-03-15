# Upstream SQLite Grammar

- **Source repository:** https://github.com/bkiers/sqlite-parser
- **Project:** sqlite-parser; an ANTLR4 grammar for SQLite
- **Attribution:** Bart Kiers, Martin Mirchev, Mike Lische (see file headers in `.g4` files)

## Vendored files

- `src/main/antlr/SQLiteLexer.g4`
- `src/main/antlr/SQLiteParser.g4`

## Local deviations

1. **Package header added:** Both lexer and parser use `@header { package io.github.e4c5.sqool.grammar.sqlite.generated; }` so that generated sources live in `io.github.e4c5.sqool.grammar.sqlite.generated`.

2. **Generated sources excluded from Checkstyle/Javadoc:** Configured in `build.gradle.kts` so that quality checks focus on sqool-owned code.

## Notes

The grammar was vendored from the sqlite-parser project. The exact upstream commit is not recorded; the grammar is used as a stable baseline for the SQLite dialect in sqool. For the canonical source and history, see the upstream repository.
