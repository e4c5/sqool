package io.github.e4c5.sqool.dialect.sqlite;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.ast.JoinTableReference;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class SqliteJoinNormalizationTest {

  private static final SqliteSqlParser PARSER = new SqliteSqlParser();

  @Test
  void crossJoinIsNormalized() {
    String sql = "SELECT * FROM t1 CROSS JOIN t2";
    ParseResult result = PARSER.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();

    SelectStatement select = assertOf(SelectStatement.class, stmt);
    assertOf(JoinTableReference.class, select.from());
  }

  @Test
  void naturalJoinIsNormalized() {
    String sql = "SELECT * FROM t1 NATURAL JOIN t2";
    ParseResult result = PARSER.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();

    SelectStatement select = assertOf(SelectStatement.class, stmt);
    JoinTableReference join = assertOf(JoinTableReference.class, select.from());
    assertTrue(join.natural(), "NATURAL JOIN flag should be set");
  }

  private <T> T assertOf(Class<T> type, Object obj) {
    assertInstanceOf(type, obj);
    return type.cast(obj);
  }

  @Test
  void simpleFromSucceeds() {
    String sql = "SELECT * FROM t1";
    ParseResult result = PARSER.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));
    assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = (Statement) ((ParseSuccess) result).root();
    assertInstanceOf(SelectStatement.class, stmt);
    SelectStatement select = (SelectStatement) stmt;
    assertNotNull(select.from());
  }
}
