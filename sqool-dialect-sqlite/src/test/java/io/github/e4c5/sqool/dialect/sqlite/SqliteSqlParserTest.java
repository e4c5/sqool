package io.github.e4c5.sqool.dialect.sqlite;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class SqliteSqlParserTest {

  private final SqliteSqlParser parser = new SqliteSqlParser();

  @Test
  void returnsFailureWithNotImplementedDiagnostic() {
    var result = parser.parse("SELECT 1", ParseOptions.defaults(SqlDialect.SQLITE));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertNotNull(failure.diagnostics());
    assertNotNull(failure.diagnostics().getFirst().message());
  }
}
