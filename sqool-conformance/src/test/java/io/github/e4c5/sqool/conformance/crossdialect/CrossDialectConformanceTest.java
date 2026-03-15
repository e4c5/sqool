package io.github.e4c5.sqool.conformance.crossdialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.ast.CreateTableStatement;
import io.github.e4c5.sqool.ast.InsertStatement;
import io.github.e4c5.sqool.ast.JoinTableReference;
import io.github.e4c5.sqool.ast.JoinType;
import io.github.e4c5.sqool.ast.OracleRawStatement;
import io.github.e4c5.sqool.ast.OracleStatementKind;
import io.github.e4c5.sqool.ast.PostgresqlRawStatement;
import io.github.e4c5.sqool.ast.PostgresqlStatementKind;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.SqliteRawStatement;
import io.github.e4c5.sqool.ast.SqliteStatementKind;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.core.DiagnosticSeverity;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.core.SyntaxDiagnostic;
import io.github.e4c5.sqool.dialect.mysql.MysqlSqlParser;
import io.github.e4c5.sqool.dialect.oracle.OracleSqlParser;
import io.github.e4c5.sqool.dialect.postgresql.PostgresqlSqlParser;
import io.github.e4c5.sqool.dialect.sqlite.SqliteSqlParser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cross-dialect conformance tests for shared constructs across MySQL, SQLite, PostgreSQL, and
 * Oracle. Asserts that equivalent queries parse successfully in all applicable dialects and produce
 * consistent AST shapes where applicable.
 */
class CrossDialectConformanceTest {

  private final MysqlSqlParser mysqlParser = new MysqlSqlParser();
  private final SqliteSqlParser sqliteParser = new SqliteSqlParser();
  private final PostgresqlSqlParser postgresqlParser = new PostgresqlSqlParser();
  private final OracleSqlParser oracleParser = new OracleSqlParser();

  // =========================================================================
  // Simple SELECT – common to all four dialects
  // =========================================================================

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

  @ParameterizedTest(name = "simple SELECT parses in all four dialects: {0}")
  @ValueSource(strings = {"SELECT id, name FROM users", "SELECT * FROM products"})
  void simpleSelectParsesInAllFourDialects(String sql) {
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));
    ParseResult postgresqlResult =
        postgresqlParser.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseResult oracleResult = oracleParser.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));

    assertSuccessWithSelectStatement(mysqlResult, "MySQL");
    assertSuccessWithSelectStatement(sqliteResult, "SQLite");
    assertSuccessWithSelectStatement(postgresqlResult, "PostgreSQL");
    assertSuccessWithSelectStatement(oracleResult, "Oracle");
  }

  // =========================================================================
  // SELECT with WHERE and ORDER BY
  // =========================================================================

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
  void selectWithWhereParsesSuccessfullyInAllFourDialects() {
    // All four dialects should parse this SQL successfully. MySQL, PostgreSQL, and Oracle
    // normalize it to SelectStatement; SQLite may fall back to SqliteRawStatement because
    // its v1 expression mapper has limited support for WHERE conditions.
    String sql = "SELECT id, name FROM users WHERE id = 1";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));
    ParseResult postgresqlResult =
        postgresqlParser.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseResult oracleResult = oracleParser.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));

    // All must succeed with no diagnostics.
    assertSuccessNoDiagnostics(mysqlResult, "MySQL");
    assertSuccessNoDiagnostics(sqliteResult, "SQLite");
    assertSuccessNoDiagnostics(postgresqlResult, "PostgreSQL");
    assertSuccessNoDiagnostics(oracleResult, "Oracle");

    // MySQL, PostgreSQL, and Oracle normalize to SelectStatement.
    assertSuccessWithSelectStatement(mysqlResult, "MySQL");
    assertSuccessWithSelectStatement(postgresqlResult, "PostgreSQL");
    assertSuccessWithSelectStatement(oracleResult, "Oracle");
  }

  // =========================================================================
  // SELECT with JOIN
  // =========================================================================

  @Test
  void selectWithJoinParsesInBothDialects() {
    String sql = "SELECT u.id, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    assertInstanceOf(ParseSuccess.class, mysqlResult);
    assertInstanceOf(ParseSuccess.class, sqliteResult);
  }

  @Test
  void selectWithJoinParsesSuccessfullyInAllFourDialects() {
    // All four dialects should parse this SQL successfully. MySQL, PostgreSQL, and Oracle
    // normalize JOINs to SelectStatement + JoinTableReference; SQLite currently falls back
    // to SqliteRawStatement for JOIN queries (known v1 limitation).
    String sql = "SELECT u.id, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));
    ParseResult postgresqlResult =
        postgresqlParser.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseResult oracleResult = oracleParser.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));

    // All must succeed with no diagnostics.
    assertSuccessNoDiagnostics(mysqlResult, "MySQL");
    assertSuccessNoDiagnostics(sqliteResult, "SQLite");
    assertSuccessNoDiagnostics(postgresqlResult, "PostgreSQL");
    assertSuccessNoDiagnostics(oracleResult, "Oracle");

    // MySQL, PostgreSQL, and Oracle normalize JOINs to SelectStatement.
    assertSuccessWithSelectStatement(mysqlResult, "MySQL");
    assertSuccessWithSelectStatement(postgresqlResult, "PostgreSQL");
    assertSuccessWithSelectStatement(oracleResult, "Oracle");
  }

  @Test
  void selectWithInnerJoinProducesJoinTableReferenceInAllFourDialects() {
    String sql = "SELECT u.id, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult postgresqlResult =
        postgresqlParser.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseResult oracleResult = oracleParser.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));

    SelectStatement mysqlStmt =
        assertInstanceOf(
            SelectStatement.class, ((ParseSuccess) mysqlResult).root(), "MySQL SELECT");
    SelectStatement pgStmt =
        assertInstanceOf(
            SelectStatement.class, ((ParseSuccess) postgresqlResult).root(), "PostgreSQL SELECT");
    SelectStatement oraStmt =
        assertInstanceOf(
            SelectStatement.class, ((ParseSuccess) oracleResult).root(), "Oracle SELECT");

    JoinTableReference mysqlJoin =
        assertInstanceOf(JoinTableReference.class, mysqlStmt.from(), "MySQL FROM");
    JoinTableReference pgJoin =
        assertInstanceOf(JoinTableReference.class, pgStmt.from(), "PostgreSQL FROM");
    JoinTableReference oraJoin =
        assertInstanceOf(JoinTableReference.class, oraStmt.from(), "Oracle FROM");

    assertEquals(JoinType.INNER, mysqlJoin.joinType());
    assertEquals(JoinType.INNER, pgJoin.joinType());
    assertEquals(JoinType.INNER, oraJoin.joinType());
  }

  // =========================================================================
  // CREATE TABLE
  // =========================================================================

  @Test
  void createTableParsesInBothDialects() {
    // Minimal CREATE TABLE using syntax common to MySQL and SQLite (INTEGER, TEXT).
    String sql = "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, email TEXT)";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    ParseSuccess mysqlSuccess = assertInstanceOf(ParseSuccess.class, mysqlResult);
    ParseSuccess sqliteSuccess = assertInstanceOf(ParseSuccess.class, sqliteResult);
    assertTrue(mysqlSuccess.diagnostics().isEmpty());
    assertTrue(sqliteSuccess.diagnostics().isEmpty());

    // MySQL normalizes CREATE TABLE into a structured AST node.
    Statement mysqlStmt = (Statement) mysqlSuccess.root();
    assertInstanceOf(CreateTableStatement.class, mysqlStmt);

    // SQLite uses a raw statement wrapper for non-SELECT statements; verify the correct kind.
    Statement sqliteStmt = (Statement) sqliteSuccess.root();
    SqliteRawStatement sqliteRaw = assertInstanceOf(SqliteRawStatement.class, sqliteStmt);
    assertEquals(SqliteStatementKind.CREATE_TABLE, sqliteRaw.kind());
  }

  @Test
  void createTableParsesInPostgresqlAndProducesRawStatement() {
    String sql = "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)";
    ParseResult pgResult =
        postgresqlParser.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL));

    ParseSuccess pgSuccess = assertInstanceOf(ParseSuccess.class, pgResult);
    assertTrue(pgSuccess.diagnostics().isEmpty());
    PostgresqlRawStatement pgRaw =
        assertInstanceOf(
            PostgresqlRawStatement.class,
            pgSuccess.root(),
            "PostgreSQL CREATE TABLE falls back to raw");
    assertEquals(PostgresqlStatementKind.CREATE_TABLE, pgRaw.kind());
  }

  @Test
  void createTableParsesInOracleAndProducesRawStatement() {
    // Oracle uses NUMBER and VARCHAR2 types.
    String sql = "CREATE TABLE employees (id NUMBER(10) PRIMARY KEY, name VARCHAR2(100))";
    ParseResult oraResult = oracleParser.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));

    ParseSuccess oraSuccess = assertInstanceOf(ParseSuccess.class, oraResult);
    assertTrue(oraSuccess.diagnostics().isEmpty());
    OracleRawStatement oraRaw =
        assertInstanceOf(
            OracleRawStatement.class, oraSuccess.root(), "Oracle CREATE TABLE falls back to raw");
    assertEquals(OracleStatementKind.CREATE_TABLE, oraRaw.kind());
  }

  // =========================================================================
  // INSERT
  // =========================================================================

  @Test
  void insertStatementParsesInBothDialects() {
    String sql = "INSERT INTO users (id, name, email) VALUES (1, 'alice', 'alice@example.com')";
    ParseResult mysqlResult = mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL));
    ParseResult sqliteResult = sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE));

    ParseSuccess mysqlSuccess = assertInstanceOf(ParseSuccess.class, mysqlResult);
    ParseSuccess sqliteSuccess = assertInstanceOf(ParseSuccess.class, sqliteResult);
    assertTrue(mysqlSuccess.diagnostics().isEmpty());
    assertTrue(sqliteSuccess.diagnostics().isEmpty());

    // MySQL normalizes INSERT into a structured AST node.
    Statement mysqlStmt = (Statement) mysqlSuccess.root();
    assertInstanceOf(InsertStatement.class, mysqlStmt);

    // SQLite uses a raw statement wrapper for non-SELECT statements; verify the correct kind.
    Statement sqliteStmt = (Statement) sqliteSuccess.root();
    SqliteRawStatement sqliteRaw = assertInstanceOf(SqliteRawStatement.class, sqliteStmt);
    assertEquals(SqliteStatementKind.INSERT, sqliteRaw.kind());
  }

  @Test
  void insertParsesInPostgresqlAndOracleAndProducesRawStatement() {
    String sql = "INSERT INTO users (id, name) VALUES (1, 'alice')";

    ParseResult pgResult =
        postgresqlParser.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseResult oraResult = oracleParser.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE));

    ParseSuccess pgSuccess = assertInstanceOf(ParseSuccess.class, pgResult, "PostgreSQL INSERT");
    assertTrue(pgSuccess.diagnostics().isEmpty());
    PostgresqlRawStatement pgRaw = assertInstanceOf(PostgresqlRawStatement.class, pgSuccess.root());
    assertEquals(PostgresqlStatementKind.INSERT, pgRaw.kind());

    ParseSuccess oraSuccess = assertInstanceOf(ParseSuccess.class, oraResult, "Oracle INSERT");
    assertTrue(oraSuccess.diagnostics().isEmpty());
    OracleRawStatement oraRaw = assertInstanceOf(OracleRawStatement.class, oraSuccess.root());
    assertEquals(OracleStatementKind.INSERT, oraRaw.kind());
  }

  // =========================================================================
  // Diagnostic consistency across all four dialects
  // =========================================================================

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

  @Test
  void invalidSelectProducesStructuredDiagnosticsInAllFourDialects() {
    // "SELECT FROM users" is syntactically invalid in all four dialects.
    String sql = "SELECT FROM users";
    List<ParseResult> results =
        List.of(
            mysqlParser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL)),
            sqliteParser.parse(sql, ParseOptions.defaults(SqlDialect.SQLITE)),
            postgresqlParser.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL)),
            oracleParser.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE)));

    List<String> dialectNames = List.of("MySQL", "SQLite", "PostgreSQL", "Oracle");
    for (int i = 0; i < results.size(); i++) {
      String dialect = dialectNames.get(i);
      ParseFailure failure =
          assertInstanceOf(ParseFailure.class, results.get(i), dialect + " should fail");
      assertFalse(failure.diagnostics().isEmpty(), dialect + " should have diagnostics");

      SyntaxDiagnostic diag = failure.diagnostics().getFirst();
      assertEquals(
          DiagnosticSeverity.ERROR,
          diag.severity(),
          dialect + " diagnostic severity should be ERROR");
      assertTrue(diag.line() >= 1, dialect + " diagnostic line should be >= 1");
      assertTrue(diag.column() >= 0, dialect + " diagnostic column should be >= 0");
      assertFalse(diag.message().isBlank(), dialect + " diagnostic message should not be blank");
    }
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private static void assertSuccessWithSelectStatement(ParseResult result, String dialectLabel) {
    ParseSuccess success =
        assertInstanceOf(ParseSuccess.class, result, dialectLabel + " parse should succeed");
    assertTrue(success.diagnostics().isEmpty(), dialectLabel + " parse should have no diagnostics");
    assertInstanceOf(
        SelectStatement.class, success.root(), dialectLabel + " root should be SelectStatement");
  }

  private static void assertSuccessNoDiagnostics(ParseResult result, String dialectLabel) {
    ParseSuccess success =
        assertInstanceOf(ParseSuccess.class, result, dialectLabel + " parse should succeed");
    assertTrue(success.diagnostics().isEmpty(), dialectLabel + " parse should have no diagnostics");
  }
}
