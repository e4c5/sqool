package io.github.e4c5.sqool.conformance.mysql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.dialect.mysql.MysqlSqlParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MysqlConformanceTest {
  private static final List<ResourceCase> SUPPORTED_CASES =
      List.of(
          new ResourceCase("mysql/supported/basic-select.sql", false),
          new ResourceCase("mysql/supported/begin-work-script.sql", true),
          new ResourceCase("mysql/supported/create-database.sql", false),
          new ResourceCase("mysql/supported/create-table-constraint.sql", false),
          new ResourceCase("mysql/supported/create-table.sql", false),
          new ResourceCase("mysql/supported/delete-statement.sql", false),
          new ResourceCase("mysql/supported/drop-table.sql", false),
          new ResourceCase("mysql/supported/drop-view.sql", false),
          new ResourceCase("mysql/supported/grouped-aggregate.sql", false),
          new ResourceCase("mysql/supported/insert-statement.sql", false),
          new ResourceCase("mysql/supported/mixed-script.sql", true),
          new ResourceCase("mysql/supported/regex-predicate.sql", false),
          new ResourceCase("mysql/supported/replace-statement.sql", false),
          new ResourceCase("mysql/supported/runtime-format.sql", false),
          new ResourceCase("mysql/supported/runtime-functions.sql", false),
          new ResourceCase("mysql/supported/show-statements.sql", false),
          new ResourceCase("mysql/supported/truncate-table.sql", false),
          new ResourceCase("mysql/supported/union-derived-join.sql", false),
          new ResourceCase("mysql/supported/update-multi-table.sql", false),
          new ResourceCase("mysql/supported/update-statement.sql", false),
          new ResourceCase("mysql/supported/select-script.sql", true));

  private static final List<ResourceExpectation> UNSUPPORTED_CASES =
      List.of(
          new ResourceExpectation("mysql/unsupported/invalid-select.sql", false, ""),
          new ResourceExpectation("mysql/unsupported/invalid-create-table.sql", false, ""));

  private final MysqlSqlParser parser = new MysqlSqlParser();

  @ParameterizedTest(name = "parses supported resource: {0}")
  @MethodSource("supportedCasesProvider")
  void parsesSupportedMysqlCorpus(ResourceCase resourceCase) {
    var sql = readResource(resourceCase.path());
    var result =
        parser.parse(
            sql, ParseOptions.defaults(SqlDialect.MYSQL).withScriptMode(resourceCase.scriptMode()));

    var success =
        assertInstanceOf(
            ParseSuccess.class, result, "Expected success for resource " + resourceCase.path());
    assertTrue(success.diagnostics().isEmpty(), "Unexpected diagnostics for " + resourceCase.path());
  }

  @Test
  void rejectsUnsupportedMysqlCorpus() {
    for (var resourceCase : UNSUPPORTED_CASES) {
      var sql = readResource(resourceCase.path());
      var result =
          parser.parse(
              sql,
              ParseOptions.defaults(SqlDialect.MYSQL).withScriptMode(resourceCase.scriptMode()));

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

  private static Stream<ResourceCase> supportedCasesProvider() {
    return SUPPORTED_CASES.stream();
  }

  private String readResource(String path) {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
      if (stream == null) {
        throw new IllegalArgumentException("Missing resource: " + path);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read resource: " + path, exception);
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
