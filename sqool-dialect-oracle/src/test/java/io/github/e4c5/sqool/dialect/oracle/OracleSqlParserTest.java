package io.github.e4c5.sqool.dialect.oracle;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class OracleSqlParserTest {

  private final OracleSqlParser parser = new OracleSqlParser();

  @Test
  void returnsFailureWithNotImplementedDiagnostic() {
    var result = parser.parse("SELECT 1 FROM dual", ParseOptions.defaults(SqlDialect.ORACLE));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertNotNull(failure.diagnostics());
    assertNotNull(failure.diagnostics().getFirst().message());
  }
}
