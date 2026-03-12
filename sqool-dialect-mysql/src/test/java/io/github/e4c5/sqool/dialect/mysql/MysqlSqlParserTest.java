package io.github.e4c5.sqool.dialect.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.IdentifierExpression;
import io.github.e4c5.sqool.ast.NamedTableReference;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class MysqlSqlParserTest {
  private final MysqlSqlParser parser = new MysqlSqlParser();

  @Test
  void parsesSimpleSelectIntoAst() {
    var result = parser.parse("select id, name from demo", ParseOptions.defaults(SqlDialect.MYSQL));

    var success = assertInstanceOf(ParseSuccess.class, result);
    var statement = assertInstanceOf(SelectStatement.class, success.root());

    assertEquals(2, statement.selectItems().size());
    assertEquals("demo", assertInstanceOf(NamedTableReference.class, statement.from()).name());
    assertEquals(
        "id",
        assertInstanceOf(
                IdentifierExpression.class,
                assertInstanceOf(ExpressionSelectItem.class, statement.selectItems().get(0))
                    .expression())
            .text());
  }

  @Test
  void parsesAllColumnsProjection() {
    var result = parser.parse("SELECT * FROM demo", ParseOptions.defaults(SqlDialect.MYSQL));

    var success = assertInstanceOf(ParseSuccess.class, result);
    var statement = assertInstanceOf(SelectStatement.class, success.root());

    assertEquals(1, statement.selectItems().size());
    assertInstanceOf(AllColumnsSelectItem.class, statement.selectItems().get(0));
  }

  @Test
  void reportsUnsupportedSelectShapes() {
    var result = parser.parse("select 1 from demo", ParseOptions.defaults(SqlDialect.MYSQL));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertFalse(failure.diagnostics().isEmpty());
    assertTrue(
        failure
            .diagnostics()
            .getFirst()
            .message()
            .contains("identifier select items without aliases"));
  }

  @Test
  void reportsSyntaxErrors() {
    var result = parser.parse("select from demo", ParseOptions.defaults(SqlDialect.MYSQL));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertFalse(failure.diagnostics().isEmpty());
  }

  @Test
  void rejectsScriptModeForNow() {
    var result =
        parser.parse(
            "select id from demo; select name from other;",
            ParseOptions.defaults(SqlDialect.MYSQL).withScriptMode(true));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertEquals(
        "MySQL MVP does not support script mode yet.", failure.diagnostics().getFirst().message());
  }
}
