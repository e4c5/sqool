package io.github.e4c5.sqool.dialect.postgresql;

import io.github.e4c5.sqool.core.DiagnosticSeverity;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.core.SqlParser;
import io.github.e4c5.sqool.core.SyntaxDiagnostic;
import java.util.List;

/**
 * Placeholder PostgreSQL parser implementation.
 *
 * <p>This stub satisfies the {@link SqlParser} contract and returns a clear {@link ParseFailure}
 * until the real PostgreSQL grammar and AST mapping are implemented.
 */
public final class PostgresqlSqlParser implements SqlParser {

  @Override
  public ParseResult parse(String sql, ParseOptions options) {
    return new ParseFailure(
        SqlDialect.POSTGRESQL,
        List.of(
            new SyntaxDiagnostic(
                DiagnosticSeverity.ERROR,
                "PostgreSQL dialect is not yet implemented.",
                1,
                0,
                null)));
  }
}
