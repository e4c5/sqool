# Diagnostics Behavior

This document describes the unified diagnostic model used by sqool across all supported SQL dialects.

## Diagnostic structure

All parser failures produce one or more `SyntaxDiagnostic` records. Each diagnostic has the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `severity` | `DiagnosticSeverity` | Always `ERROR` for syntax failures. |
| `message` | `String` | Human-readable description of the syntax error, provided by ANTLR's default error listener. |
| `line` | `int` | 1-based line number of the offending token (clamped to `>= 1`). |
| `column` | `int` | 0-based column offset of the offending token within the line (clamped to `>= 0`). |
| `offendingToken` | `String` | Text of the token that triggered the error, or `null` if no token was available. |

All fields except `offendingToken` are always non-null. `message` is never blank.

## Consistency across dialects

All four dialects (MySQL, SQLite, PostgreSQL, Oracle) use the same `AntlrSyntaxErrorListener` to collect diagnostics. This listener is wired into both the lexer and the parser, so lexer-level and parser-level errors are captured uniformly.

For analogous invalid SQL (e.g. `SELECT FROM users`), all four dialects:
- Return a `ParseFailure` result.
- Include at least one `SyntaxDiagnostic`.
- Set `severity = ERROR`.
- Provide a non-blank, non-null message.
- Provide `line >= 1` and `column >= 0`.

### Dialect-specific message text

The `message` field originates from ANTLR's built-in error reporting and includes the grammar-specific rule names and token names. Message text therefore differs between dialects for equivalent errors. Consumers should not rely on message text for programmatic logic; use the structured fields (`line`, `column`, `offendingToken`) for precise error location.

### `offendingToken`

For most parse errors, ANTLR supplies the offending token and it is reported as a non-null string. For edge cases (e.g. end-of-file reached unexpectedly), `offendingToken` may be `null` or `"<EOF>"`.

## Diagnostic collection flow

```
SQL Input
  ↓
[Lexer]  ─── lexer error ──→ AntlrSyntaxErrorListener.syntaxError(...)
  ↓
[Parser] ─── parser error ─→ AntlrSyntaxErrorListener.syntaxError(...)
  ↓
ParseAttempt(context, diagnostics)
  ↓
if diagnostics non-empty → ParseFailure(dialect, diagnostics, metrics)
else                     → ParseSuccess(dialect, astRoot, diagnostics, metrics)
```

Both SLL and LL parse attempts use the same listener instance. If the SLL attempt fails (throws `ParseCancellationException`), a fresh LL attempt is made with a fresh `AntlrSyntaxErrorListener`. Diagnostics from the failed SLL attempt are discarded; only LL diagnostics are propagated to the result.

## SLL fast-path and diagnostics

When `ParseOptions.enableFallback` is `false` and the SLL attempt fails, a synthetic diagnostic is added instead:
- MySQL: `"Fast-path MySQL parse failed."` at line 1, column 0, no offending token.
- PostgreSQL: `"Fast-path PostgreSQL parse failed."` at line 1, column 0.
- Oracle: `"Fast-path Oracle parse failed."` or `"Fast-path Oracle script parse failed."` at line 1, column 0.
- SQLite: `"Fast-path SQLite parse failed."` at line 1, column 0.

This behavior is intentional: when fallback is disabled, the parser prioritizes speed over diagnostic richness, and only LL-generated diagnostics are available.

## Known dialect differences

| Dialect | Notes |
|---------|-------|
| MySQL | Message format includes MySQL grammar token names (e.g. `IDENTIFIER_SYMBOL`, `FROM_SYMBOL`). |
| SQLite | Message format uses SQLite grammar token names. |
| PostgreSQL | Message format uses PostgreSQL grammar token names. |
| Oracle | Message format uses Oracle grammar token names. |

The structural fields (`line`, `column`, `offendingToken`, `severity`) are consistent across all dialects and can be relied upon for error location and display.

## Example

```java
SqlParser parser = new MysqlSqlParser();
ParseResult result = parser.parse("SELECT FROM users", ParseOptions.defaults(SqlDialect.MYSQL));

if (result instanceof ParseFailure failure) {
    SyntaxDiagnostic d = failure.diagnostics().getFirst();
    // d.severity()       → ERROR
    // d.line()           → 1
    // d.column()         → 7
    // d.offendingToken() → "FROM"
    // d.message()        → "no viable alternative at input 'select from'"
}
```

The same structural contract holds for all four dialects.
