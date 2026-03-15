package io.github.e4c5.sqool.conformance.crossdialect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.core.ParseFailure;
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
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

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
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    assertInstanceOf(ParseSuccess.class, mysqlResult);
    assertInstanceOf(ParseSuccess.class, sqliteResult);
    assertTrue(((ParseSuccess) mysqlResult).diagnostics().isEmpty());
    assertTrue(((ParseSuccess) sqliteResult).diagnostics().isEmpty());
  }

  @Test
  void selectWithJoinParsesInBothDialects() {
    String sql = "SELECT u.id, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    assertInstanceOf(ParseSuccess.class, mysqlResult);
    assertInstanceOf(ParseSuccess.class, sqliteResult);
  }

  @Test
  void createTableParsesInBothDialects() {
    // Minimal CREATE TABLE using syntax common to MySQL and SQLite (INTEGER, TEXT).
    String sql = "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, email TEXT)";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    assertInstanceOf(ParseSuccess.class, mysqlResult);
    assertInstanceOf(ParseSuccess.class, sqliteResult);
    assertTrue(((ParseSuccess) mysqlResult).diagnostics().isEmpty());
    assertTrue(((ParseSuccess) sqliteResult).diagnostics().isEmpty());
  }

  @Test
  void insertStatementParsesInBothDialects() {
    String sql = "INSERT INTO users (id, name, email) VALUES (1, 'alice', 'alice@example.com')";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    assertInstanceOf(ParseSuccess.class, mysqlResult);
    assertInstanceOf(ParseSuccess.class, sqliteResult);
    assertTrue(((ParseSuccess) mysqlResult).diagnostics().isEmpty());
    assertTrue(((ParseSuccess) sqliteResult).diagnostics().isEmpty());
  }

  @Test
  void analogousSyntaxErrorProducesConsistentDiagnosticStructure() {
    // Invalid SQL: SELECT without column list. Both dialects should fail with structured
    // diagnostics.
    String sql = "SELECT FROM users";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    ParseFailure mysqlFailure = assertInstanceOf(ParseFailure.class, mysqlResult);
    ParseFailure sqliteFailure = assertInstanceOf(ParseFailure.class, sqliteResult);

    assertFalse(mysqlFailure.diagnostics().isEmpty());
    assertFalse(sqliteFailure.diagnostics().isEmpty());

    var mysqlDiag = mysqlFailure.diagnostics().getFirst();
    var sqliteDiag = sqliteFailure.diagnostics().getFirst();

    assertTrue(mysqlDiag.line() >= 1);
    assertTrue(sqliteDiag.line() >= 1);
    assertTrue(mysqlDiag.column() >= 0);
    assertTrue(sqliteDiag.column() >= 0);
    assertFalse(mysqlDiag.message().isBlank());
    assertFalse(sqliteDiag.message().isBlank());
    assertNotNull(mysqlDiag.severity());
    assertNotNull(sqliteDiag.severity());
  }
}
