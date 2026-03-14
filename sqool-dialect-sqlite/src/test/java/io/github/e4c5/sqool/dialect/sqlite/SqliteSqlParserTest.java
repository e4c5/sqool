package io.github.e4c5.sqool.dialect.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.LiteralExpression;
import io.github.e4c5.sqool.ast.NamedTableReference;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.SqliteRawStatement;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class SqliteSqlParserTest {

  private static final SqliteSqlParser PARSER = new SqliteSqlParser();

  @Test
  void parseSimpleSelectLiteral() {
    ParseResult result = PARSER.parse("SELECT 1", ParseOptions.defaults(SqlDialect.SQLITE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();
    assertInstanceOf(SelectStatement.class, stmt);
    SelectStatement select = (SelectStatement) stmt;
    assertEquals(1, select.selectItems().size());
    assertInstanceOf(ExpressionSelectItem.class, select.selectItems().get(0));
    assertInstanceOf(
        LiteralExpression.class, ((ExpressionSelectItem) select.selectItems().get(0)).expression());
    assertEquals(
        "1",
        ((LiteralExpression) ((ExpressionSelectItem) select.selectItems().get(0)).expression())
            .text());
  }

  @Test
  void parseSelectStarFromTable() {
    ParseResult result = PARSER.parse("SELECT * FROM t", ParseOptions.defaults(SqlDialect.SQLITE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();
    assertInstanceOf(SelectStatement.class, stmt);
    SelectStatement select = (SelectStatement) stmt;
    assertEquals(1, select.selectItems().size());
    assertNotNull(select.from());
    assertInstanceOf(NamedTableReference.class, select.from());
    assertEquals("t", ((NamedTableReference) select.from()).name());
  }

  @Test
  void parseCreateTableAsRaw() {
    ParseResult result =
        PARSER.parse("CREATE TABLE x (a INT);", ParseOptions.defaults(SqlDialect.SQLITE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();
    assertInstanceOf(SqliteRawStatement.class, stmt);
    assertTrue(((SqliteRawStatement) stmt).sqlText().contains("CREATE"));
  }

  @Test
  void rejectsNullSql() {
    ParseResult result = PARSER.parse(null, ParseOptions.defaults(SqlDialect.SQLITE));
    assertTrue(result instanceof io.github.e4c5.sqool.core.ParseFailure);
  }

  @Test
  void rejectsWrongDialectOption() {
    ParseResult result = PARSER.parse("SELECT 1", ParseOptions.defaults(SqlDialect.MYSQL));
    assertTrue(result instanceof io.github.e4c5.sqool.core.ParseFailure);
  }
}
