# PostgreSQL Grammar Notes – Milestone 3

## 1. Purpose

This document summarises key quality, ambiguity, and design observations from the
Milestone 3 work on the PostgreSQL ANTLR grammar (`sqool-grammar-postgresql`).
It records decisions made, fixes applied, and issues that are deliberately deferred.

## 2. Grammar structure overview

The fork consists of two grammar files:

| File | Role |
|------|------|
| `PostgreSQLLexer.g4` | Case-insensitive lexer for tokens in the v1 subset |
| `PostgreSQLParser.g4` | Parser grammar with two entry points: `root` (script) and `singleStatement` |

Entry points:

- **`singleStatement`**: Used by the single-statement parse path (default).
- **`root`**: Used in script mode; handles multiple semicolon-separated statements.

## 3. Key ambiguity and quality findings

### 3.1 BETWEEN…AND conflict (risk: medium, status: mitigated)

Standard SQL BETWEEN syntax is inherently ambiguous with the binary AND operator:

```sql
a BETWEEN 1 AND 5 AND b = 2
```

In ANTLR4 left-recursive rules, the alternative ordering controls precedence.
`betweenExpr` is placed before `andExpr`, giving BETWEEN higher precedence.
This ensures `AND` in a BETWEEN clause binds to the high bound, not the outer expression.

*Regression test*: `select-where.sql` includes expressions with BETWEEN that exercise
the parser's ability to correctly parse the precedence boundary.

### 3.2 Dollar-quoted string literals (risk: low, status: partially mitigated)

PostgreSQL supports tagged dollar-quoting (`$tag$...$tag$`), which requires a stateful
lexer predicate to match the opening and closing tag.

*Decision*: Only plain `$$...$$` is supported in the v1 grammar. Tagged dollar quotes
will tokenize as an `IDENTIFIER` followed by a lexer error, falling through to the
raw-statement path. The impact on typical application SQL is low.

*Workaround*: Tagged dollar quotes appear mainly in PL/pgSQL function bodies, which are
outside the v1 subset anyway.

### 3.3 :: cast operator vs. bitwise shift (risk: low, status: mitigated)

The PostgreSQL-specific cast operator `::` uses two colons. The lexer defines
`CAST_OP : '::';` before any single-colon token, relying on ANTLR4's maximal-munch
rule to correctly tokenize `::` as a single token.

*Status*: Grammar compiles and tests pass; no ambiguity observed.

### 3.4 Keyword / identifier conflicts (risk: medium, status: mitigated)

PostgreSQL uses many words as both keywords and valid identifiers (unreserved keywords).
For example, `TEXT`, `TIME`, `UUID`, `RETURNING`, and many type names can appear as
column or table names.

*Resolution*: The `unreservedKeyword` rule in the parser enumerates all token types
that may appear as identifiers without syntactic ambiguity for the v1 subset.
The `name` rule matches `IDENTIFIER | QUOTED_IDENTIFIER | unreservedKeyword`.

*Known gap*: Some keywords are not yet in `unreservedKeyword`. Queries using those
keywords as identifiers will fall back to the raw-statement path rather than failing.

### 3.5 FROM clause join ambiguity (risk: low for v1, status: accepted)

The `tableReference` rule supports both simple named tables and join chains.
Joins are parsed correctly, but the AST mapper currently falls back to `PostgresqlRawStatement`
for any query with joins (since the normalized `JoinTableReference` mapping is deferred to M4).

*Impact*: Queries with joins return a raw statement with the correct `SELECT` kind;
no parse errors are generated.

### 3.6 Expression grammar coverage (risk: low, status: accepted)

The `expr` rule covers the most common expression forms (literals, identifiers,
arithmetic, comparison, logical, IS NULL, CAST). Complex forms (CASE WHEN, window
functions, array expressions, subqueries) are syntactically recognized but mapped
to `LiteralExpression` with raw text in the AST mapper.

*Approach*: This keeps the mapper simple while ensuring no parse failures for valid SQL.

## 4. Grammar changes vs upstream

| Change | Rationale |
|--------|-----------|
| Scoped to v1 subset (fewer statement types) | Reduces grammar size and ambiguity surface |
| Expression rule simplified (no complex CASE/window detail) | Improves SLL prediction hit rate |
| `RETURNING` as first-class rule | Easier PostgreSQL-specific AST mapping |
| Added explicit `unreservedKeyword` rule | Consistent identifier handling for keyword-identifiers |
| `NOTHING`, `KEY`, `ACTION`, `WORK`, etc. added as lexer tokens | Eliminates ANTLR "implicit definition" warnings |
| Plain `$$...$$` dollar-quote only | Avoids stateful lexer predicate; tagged form deferred |

## 5. Remaining issues and priority

| Issue | Priority | Status |
|-------|----------|--------|
| Tagged dollar-quote (`$tag$...$tag$`) not lexed | Low | Deferred to M4+ |
| Join AST mapping not implemented | Medium | Deferred to M4 |
| Complex CASE / window function AST mapping | Low | Deferred to M4 |
| `unreservedKeyword` list may be incomplete for edge-case identifiers | Low | Tracked |
| INTERVAL type field qualifiers simplified | Low | Accepted for v1 |
| ARRAY subscript mapping (`expr[idx]`) returns raw literal | Low | Accepted for v1 |

## 6. Regression test coverage

Grammar and mapping regressions are covered by:

- `sqool-grammar-postgresql` – `PostgreSQLGrammarSmokeTest` (10 cases)
- `sqool-dialect-postgresql` – `PostgresqlSqlParserTest` (23 cases)
- `sqool-conformance` – `PostgresqlConformanceTest` (11 supported + 2 failure cases)
