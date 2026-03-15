package io.github.e4c5.sqool.conformance.oracle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.dialect.oracle.OracleSqlParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Oracle conformance test suite for the v1 SQL subset. */
class OracleConformanceTest {

  private static final List<ResourceCase> SUPPORTED_CASES =
      List.of(
          new ResourceCase("oracle/supported/basic-select.sql", true),
          new ResourceCase("oracle/supported/select-where.sql", true),
          new ResourceCase("oracle/supported/select-order.sql", true),
          new ResourceCase("oracle/supported/select-join.sql", true),
          new ResourceCase("oracle/supported/insert-statement.sql", true),
          new ResourceCase("oracle/supported/update-statement.sql", true),
          new ResourceCase("oracle/supported/delete-statement.sql", true),
          new ResourceCase("oracle/supported/create-table.sql", true),
          new ResourceCase("oracle/supported/drop-table.sql", true),
          new ResourceCase("oracle/supported/transaction-control.sql", true),
          new ResourceCase("oracle/supported/mixed-script.sql", true));

  private static final List<ResourceExpectation> UNSUPPORTED_CASES =
      List.of(
          new ResourceExpectation("oracle/unsupported/invalid-select.sql", false, ""),
          new ResourceExpectation("oracle/unsupported/invalid-create-table.sql", false, ""));

  private final OracleSqlParser parser = new OracleSqlParser();

  private static Stream<ResourceCase> supportedCasesProvider() {
    return SUPPORTED_CASES.stream();
  }

  @ParameterizedTest(name = "parses supported resource: {0}")
  @MethodSource("supportedCasesProvider")
  void parsesSupportedOracleCorpus(ResourceCase resourceCase) throws IOException {
    var sql = readResource(resourceCase.path());
    var result =
        parser.parse(
            sql,
            ParseOptions.defaults(SqlDialect.ORACLE).withScriptMode(resourceCase.scriptMode()));

    var success =
        assertInstanceOf(
            ParseSuccess.class, result, "Expected success for resource " + resourceCase.path());
    assertTrue(
        success.diagnostics().isEmpty(), "Unexpected diagnostics for " + resourceCase.path());
  }

  @Test
  void rejectsUnsupportedOracleCorpus() throws IOException {
    for (var resourceCase : UNSUPPORTED_CASES) {
      var sql = readResource(resourceCase.path());
      var result =
          parser.parse(
              sql,
              ParseOptions.defaults(SqlDialect.ORACLE).withScriptMode(resourceCase.scriptMode()));

      var failure =
          assertInstanceOf(
              ParseFailure.class, result, "Expected failure for resource " + resourceCase.path());
      assertFalse(
          failure.diagnostics().isEmpty(), "Expected diagnostics for " + resourceCase.path());

      if (!resourceCase.messageFragment().isBlank()) {
        assertTrue(
            failure.diagnostics().getFirst().message().contains(resourceCase.messageFragment()),
            "Expected message fragment '"
                + resourceCase.messageFragment()
                + "' for "
                + resourceCase.path());
      }
    }
  }

  private String readResource(String path) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        throw new IllegalArgumentException("Missing resource: " + path);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private record ResourceCase(String path, boolean scriptMode) {
    @Override
    public String toString() {
      return path;
    }
  }

  private record ResourceExpectation(String path, boolean scriptMode, String messageFragment) {}
}
