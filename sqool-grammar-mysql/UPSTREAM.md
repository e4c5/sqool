# Upstream MySQL Grammar

- Source repository: `https://github.com/antlr/grammars-v4`
- Upstream path: `sql/mysql/Oracle`
- Imported commit: `7bb150f62f54e587f18e323b71fa0309bdec5056`

## Vendored files

- `src/main/antlr/MySQLLexer.g4`
- `src/main/antlr/MySQLParser.g4`
- `src/main/java/io/github/e4c5/sqool/grammar/mysql/generated/MySQLLexerBase.java`
- `src/main/java/io/github/e4c5/sqool/grammar/mysql/generated/MySQLParserBase.java`
- `src/main/java/io/github/e4c5/sqool/grammar/mysql/generated/SqlMode.java`
- `src/main/java/io/github/e4c5/sqool/grammar/mysql/generated/SqlModes.java`

## Local deviations

1. Added Java package declarations to the vendored Java support classes so they live beside the
   generated parser code in `io.github.e4c5.sqool.grammar.mysql.generated`.
2. Excluded vendored support classes and generated ANTLR sources from local Checkstyle/Javadoc
   enforcement to keep repository quality checks focused on sqool-owned code.
