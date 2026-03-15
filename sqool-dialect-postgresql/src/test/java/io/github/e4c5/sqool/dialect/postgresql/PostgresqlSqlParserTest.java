package io.github.e4c5.sqool.dialect.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.DeleteStatement;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.IdentifierExpression;
import io.github.e4c5.sqool.ast.InsertStatement;
import io.github.e4c5.sqool.ast.LiteralExpression;
import io.github.e4c5.sqool.ast.NamedTableReference;
import io.github.e4c5.sqool.ast.PostgresqlRawStatement;
import io.github.e4c5.sqool.ast.PostgresqlStatementKind;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.SqlScript;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.ast.UpdateStatement;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class PostgresqlSqlParserTest {

  private static final PostgresqlSqlParser PARSER = new PostgresqlSqlParser();

  // =========================================================================
  // Basic acceptance tests
  // =========================================================================

  @Test
  void parseSimpleSelectLiteral() {
    ParseResult result = PARSER.parse("SELECT 1", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertEquals(1, select.selectItems().size());
    ExpressionSelectItem item =
        assertInstanceOf(ExpressionSelectItem.class, select.selectItems().get(0));
    LiteralExpression lit = assertInstanceOf(LiteralExpression.class, item.expression());
    assertEquals("1", lit.text());
  }

  @Test
  void parseSelectStarFromTable() {
    ParseResult result =
        PARSER.parse("SELECT * FROM users", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertEquals(1, select.selectItems().size());
    assertInstanceOf(AllColumnsSelectItem.class, select.selectItems().get(0));
    assertNotNull(select.from());
    NamedTableReference ref = assertInstanceOf(NamedTableReference.class, select.from());
    assertEquals("users", ref.name());
    assertNull(ref.alias());
  }

  @Test
  void parseSelectWithAlias() {
    ParseResult result =
        PARSER.parse("SELECT id, name FROM users u", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    NamedTableReference ref = assertInstanceOf(NamedTableReference.class, select.from());
    assertEquals("users", ref.name());
    assertEquals("u", ref.alias());
  }

  @Test
  void parseSelectWithWhereClause() {
    ParseResult result =
        PARSER.parse(
            "SELECT id FROM users WHERE id = 1", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertNotNull(select.where());
  }

  @Test
  void parseSelectWithOrderBy() {
    ParseResult result =
        PARSER.parse(
            "SELECT id FROM users ORDER BY id DESC", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertEquals(1, select.orderBy().size());
    assertEquals(io.github.e4c5.sqool.ast.SortDirection.DESC, select.orderBy().get(0).direction());
  }

  @Test
  void parseSelectWithLimit() {
    ParseResult result =
        PARSER.parse("SELECT id FROM users LIMIT 10", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertNotNull(select.limit());
    assertEquals(10L, select.limit().rowCount());
  }

  @Test
  void parseSelectDistinct() {
    ParseResult result =
        PARSER.parse(
            "SELECT DISTINCT name FROM users", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertTrue(select.distinct());
  }

  // =========================================================================
  // Raw statement fallback for DML and DDL
  // =========================================================================

  @Test
  void parseInsertProducesNormalizedAst() {
    ParseResult result =
        PARSER.parse(
            "INSERT INTO users (name) VALUES ('Alice')",
            ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(InsertStatement.class, success.root());
  }

  @Test
  void parseUpdateProducesNormalizedAst() {
    ParseResult result =
        PARSER.parse(
            "UPDATE users SET name = 'Bob' WHERE id = 1",
            ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(UpdateStatement.class, success.root());
  }

  @Test
  void parseDeleteProducesNormalizedAst() {
    ParseResult result =
        PARSER.parse(
            "DELETE FROM users WHERE id = 1", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(DeleteStatement.class, success.root());
  }

  @Test
  void parseCreateTableAsRaw() {
    ParseResult result =
        PARSER.parse(
            "CREATE TABLE users (id SERIAL PRIMARY KEY, name TEXT NOT NULL)",
            ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    PostgresqlRawStatement raw = assertInstanceOf(PostgresqlRawStatement.class, success.root());
    assertEquals(PostgresqlStatementKind.CREATE_TABLE, raw.kind());
    assertTrue(raw.sqlText().contains("CREATE"));
  }

  @Test
  void parseDropTableAsRaw() {
    ParseResult result =
        PARSER.parse(
            "DROP TABLE IF EXISTS users CASCADE", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    PostgresqlRawStatement raw = assertInstanceOf(PostgresqlRawStatement.class, success.root());
    assertEquals(PostgresqlStatementKind.DROP_TABLE, raw.kind());
  }

  @Test
  void parseInsertReturningAsRaw() {
    ParseResult result =
        PARSER.parse(
            "INSERT INTO users (name) VALUES ('Alice') RETURNING id",
            ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(PostgresqlRawStatement.class, success.root());
  }

  // =========================================================================
  // SELECT fallback for complex shapes
  // =========================================================================

  @Test
  void parseSelectWithJoinProducesSelectStatement() {
    ParseResult result =
        PARSER.parse(
            "SELECT u.id, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id",
            ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    // Joins are now normalized to SelectStatement + JoinTableReference.
    assertInstanceOf(SelectStatement.class, success.root());
    SelectStatement stmt = (SelectStatement) success.root();
    assertInstanceOf(
        io.github.e4c5.sqool.ast.JoinTableReference.class,
        stmt.from(),
        "FROM clause should be a JoinTableReference");
  }

  @Test
  void parseSelectWithCteAsRaw() {
    ParseResult result =
        PARSER.parse(
            "WITH cte AS (SELECT 1) SELECT * FROM cte",
            ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(PostgresqlRawStatement.class, success.root());
  }

  @Test
  void parseSelectUnionAsRaw() {
    ParseResult result =
        PARSER.parse("SELECT 1 UNION SELECT 2", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(PostgresqlRawStatement.class, success.root());
  }

  // =========================================================================
  // Script mode
  // =========================================================================

  @Test
  void parseScriptMode() {
    String sql = "SELECT 1;\nSELECT 2;";
    ParseResult result =
        PARSER.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL).withScriptMode(true));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SqlScript script = assertInstanceOf(SqlScript.class, success.root());
    assertEquals(2, script.statements().size());
  }

  @Test
  void parseScriptModeWithMixedStatements() {
    String sql =
        "CREATE TABLE t (id INT);\n" + "INSERT INTO t VALUES (1);\n" + "SELECT * FROM t;\n";
    ParseResult result =
        PARSER.parse(sql, ParseOptions.defaults(SqlDialect.POSTGRESQL).withScriptMode(true));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SqlScript script = assertInstanceOf(SqlScript.class, success.root());
    assertEquals(3, script.statements().size());
  }

  // =========================================================================
  // Validation tests
  // =========================================================================

  @Test
  void rejectsNullSql() {
    ParseResult result = PARSER.parse(null, ParseOptions.defaults(SqlDialect.POSTGRESQL));
    assertInstanceOf(ParseFailure.class, result);
  }

  @Test
  void rejectsBlankSql() {
    ParseResult result = PARSER.parse("  ", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    assertInstanceOf(ParseFailure.class, result);
  }

  @Test
  void rejectsWrongDialect() {
    ParseResult result = PARSER.parse("SELECT 1", ParseOptions.defaults(SqlDialect.MYSQL));
    assertInstanceOf(ParseFailure.class, result);
  }

  @Test
  void rejectsInvalidSyntax() {
    ParseResult result = PARSER.parse("SELECT FROM", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    assertInstanceOf(ParseFailure.class, result);
  }

  @Test
  void selectColumnWithDotNotation() {
    ParseResult result =
        PARSER.parse("SELECT u.name FROM users u", ParseOptions.defaults(SqlDialect.POSTGRESQL));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = assertInstanceOf(Statement.class, success.root());
    assertInstanceOf(SelectStatement.class, stmt);
    SelectStatement select = (SelectStatement) stmt;
    ExpressionSelectItem item =
        assertInstanceOf(ExpressionSelectItem.class, select.selectItems().get(0));
    IdentifierExpression ident = assertInstanceOf(IdentifierExpression.class, item.expression());
    assertEquals("u.name", ident.text());
  }
}
