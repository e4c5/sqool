# Upstream SQLite Grammar

- Source repository: `https://github.com/antlr/grammars-v4`
- Upstream path: `sql/sqlite`
- Imported from: master (ANTLR grammars-v4)

## Vendored files

- `src/main/antlr/SQLiteLexer.g4`
- `src/main/antlr/SQLiteParser.g4`

## Local deviations

1. Added Java package declaration `@header { package io.github.e4c5.sqool.grammar.sqlite.generated; }` to both lexer and parser so generated sources live in `io.github.e4c5.sqool.grammar.sqlite.generated`.
2. Excluded generated ANTLR sources from Checkstyle/Javadoc in this module.
