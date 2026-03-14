package io.github.e4c5.sqool.dialect.postgresql;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class PostgresqlSqlParserTest {

  private final PostgresqlSqlParser parser = new PostgresqlSqlParser();

  @Test
  void returnsFailureWithNotImplementedDiagnostic() {
    var result = parser.parse("SELECT 1", ParseOptions.defaults(SqlDialect.POSTGRESQL));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertNotNull(failure.diagnostics());
    assertNotNull(failure.diagnostics().getFirst().message());
  }
}
