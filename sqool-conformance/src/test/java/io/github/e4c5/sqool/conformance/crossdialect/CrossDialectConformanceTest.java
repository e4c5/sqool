package io.github.e4c5.sqool.conformance.crossdialect;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.dialect.mysql.MysqlSqlParser;
import io.github.e4c5.sqool.dialect.sqlite.SqliteSqlParser;
import org.junit.jupiter.api.Test;

/**
 * Cross-dialect conformance tests for shared constructs between MySQL and SQLite. Asserts that
 * equivalent queries parse successfully in both dialects and produce consistent AST shapes where
 * applicable.
 */
class CrossDialectConformanceTest {

  private final MysqlSqlParser mysqlParser = new MysqlSqlParser();
  private final SqliteSqlParser sqliteParser = new SqliteSqlParser();

  @Test
  void simpleSelectParsesInBothDialects() {
    String sql = "SELECT id, name FROM users";
    ParseResult mysqlResult =
        mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult =
        sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    ParseSuccess mysqlSuccess = assertInstanceOf(ParseSuccess.class, mysqlResult);
    ParseSuccess sqliteSuccess = assertInstanceOf(ParseSuccess.class, sqliteResult);
    assertTrue(mysqlSuccess.diagnostics().isEmpty());
    assertTrue(sqliteSuccess.diagnostics().isEmpty());

    Statement mysqlStmt = (Statement) mysqlSuccess.root();
    Statement sqliteStmt = (Statement) sqliteSuccess.root();
    assertInstanceOf(SelectStatement.class, mysqlStmt);
    assertInstanceOf(SelectStatement.class, sqliteStmt);
  }

  @Test
  void selectWithWhereAndOrderByParsesInBothDialects() {
    String sql = "SELECT * FROM users WHERE id = 1 ORDER BY name LIMIT 10";
    ParseResult mysqlResult =
        mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult =
        sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    assertInstanceOf(ParseSuccess.class, mysqlResult);
    assertInstanceOf(ParseSuccess.class, sqliteResult);
    assertTrue(((ParseSuccess) mysqlResult).diagnostics().isEmpty());
    assertTrue(((ParseSuccess) sqliteResult).diagnostics().isEmpty());
  }

  @Test
  void selectWithJoinParsesInBothDialects() {
    String sql = "SELECT u.id, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id";
    ParseResult mysqlResult =
        mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult =
        sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    assertInstanceOf(ParseSuccess.class, mysqlResult);
    assertInstanceOf(ParseSuccess.class, sqliteResult);
  }
}
