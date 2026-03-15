package io.github.e4c5.sqool.dialect.oracle;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class OracleDmlNormalizationTest {

  private static final OracleSqlParser PARSER = new OracleSqlParser();

  @Test
  void insertIsNowNormalized() {
    String sql = "INSERT INTO t (a, b) VALUES (1, 2)";
    ParseResult result = PARSER.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();
    io.github.e4c5.sqool.ast.InsertStatement insert =
        assertOf(io.github.e4c5.sqool.ast.InsertStatement.class, stmt);
    assertEquals("t", insert.tableName());
    assertEquals(2, insert.columns().size());
  }

  @Test
  void updateIsNowNormalized() {
    String sql = "UPDATE t SET a = 1 WHERE b = 2";
    ParseResult result = PARSER.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();
    io.github.e4c5.sqool.ast.UpdateStatement update =
        assertOf(io.github.e4c5.sqool.ast.UpdateStatement.class, stmt);
    assertNotNull(update.where());
  }

  @Test
  void deleteIsNowNormalized() {
    String sql = "DELETE FROM t WHERE a = 1";
    ParseResult result = PARSER.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();
    io.github.e4c5.sqool.ast.DeleteStatement delete =
        assertOf(io.github.e4c5.sqool.ast.DeleteStatement.class, stmt);
    assertNotNull(delete.where());
  }

  private <T> T assertOf(Class<T> type, Object obj) {
    assertInstanceOf(type, obj);
    return type.cast(obj);
  }

  private static void assertEquals(Object expected, Object actual) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
  }

  private static void assertNotNull(Object actual) {
    org.junit.jupiter.api.Assertions.assertNotNull(actual);
  }
}
