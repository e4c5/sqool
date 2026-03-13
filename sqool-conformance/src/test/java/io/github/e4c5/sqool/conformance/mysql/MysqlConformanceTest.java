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
import org.junit.jupiter.api.Test;

class MysqlConformanceTest {
  private static final List<ResourceCase> SUPPORTED_CASES =
      List.of(
          new ResourceCase("mysql/supported/basic-select.sql", false),
          new ResourceCase("mysql/supported/create-database.sql", false),
          new ResourceCase("mysql/supported/create-table.sql", false),
          new ResourceCase("mysql/supported/delete-statement.sql", false),
          new ResourceCase("mysql/supported/drop-table.sql", false),
          new ResourceCase("mysql/supported/grouped-aggregate.sql", false),
          new ResourceCase("mysql/supported/insert-statement.sql", false),
          new ResourceCase("mysql/supported/mixed-script.sql", true),
          new ResourceCase("mysql/supported/replace-statement.sql", false),
          new ResourceCase("mysql/supported/runtime-functions.sql", false),
          new ResourceCase("mysql/supported/show-statements.sql", false),
          new ResourceCase("mysql/supported/truncate-table.sql", false),
          new ResourceCase("mysql/supported/union-derived-join.sql", false),
          new ResourceCase("mysql/supported/update-statement.sql", false),
          new ResourceCase("mysql/supported/select-script.sql", true));

  private static final List<ResourceExpectation> UNSUPPORTED_CASES =
      List.of(
          new ResourceExpectation(
              "mysql/unsupported/create-table-constraint.sql", false, "table constraints"),
          new ResourceExpectation("mysql/unsupported/drop-view.sql", false, "DROP statement kind"),
          new ResourceExpectation(
              "mysql/unsupported/regex-predicate.sql", false, "predicate operation"),
          new ResourceExpectation(
              "mysql/unsupported/runtime-format.sql", false, "runtime function"),
          new ResourceExpectation(
              "mysql/unsupported/update-multi-table.sql", false, "single-table UPDATE"),
          new ResourceExpectation(
              "mysql/unsupported/begin-work-script.sql", true, "BEGIN WORK statements"));

  private final MysqlSqlParser parser = new MysqlSqlParser();

  @Test
  void parsesSupportedMysqlCorpus() {
    for (var resourceCase : SUPPORTED_CASES) {
      var sql = readResource(resourceCase.path());
      var result =
          parser.parse(
              sql,
              ParseOptions.defaults(SqlDialect.MYSQL).withScriptMode(resourceCase.scriptMode()));

      var success =
          assertInstanceOf(
              ParseSuccess.class, result, "Expected success for resource " + resourceCase.path());
      assertTrue(
          success.diagnostics().isEmpty(), "Unexpected diagnostics for " + resourceCase.path());
    }
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
      assertTrue(
          failure.diagnostics().getFirst().message().contains(resourceCase.messageFragment()),
          "Expected message fragment '"
              + resourceCase.messageFragment()
              + "' for "
              + resourceCase.path());
    }
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

  private record ResourceCase(String path, boolean scriptMode) {}

  private record ResourceExpectation(String path, boolean scriptMode, String messageFragment) {}
}
