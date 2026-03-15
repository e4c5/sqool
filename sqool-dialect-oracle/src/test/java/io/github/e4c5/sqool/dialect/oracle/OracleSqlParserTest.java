package io.github.e4c5.sqool.dialect.oracle;

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
import io.github.e4c5.sqool.ast.OracleRawStatement;
import io.github.e4c5.sqool.ast.OracleStatementKind;
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

class OracleSqlParserTest {

  private static final OracleSqlParser PARSER = new OracleSqlParser();

  // =========================================================================
  // Basic acceptance tests
  // =========================================================================

  @Test
  void parseSimpleSelectLiteral() {
    ParseResult result =
        PARSER.parse("SELECT 1 FROM DUAL", ParseOptions.defaults(SqlDialect.ORACLE));
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
        PARSER.parse("SELECT * FROM employees", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertEquals(1, select.selectItems().size());
    assertInstanceOf(AllColumnsSelectItem.class, select.selectItems().get(0));
    assertNotNull(select.from());
    NamedTableReference ref = assertInstanceOf(NamedTableReference.class, select.from());
    assertEquals("employees", ref.name());
    assertNull(ref.alias());
  }

  @Test
  void parseSelectWithAlias() {
    ParseResult result =
        PARSER.parse("SELECT id, name FROM employees e", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    NamedTableReference ref = assertInstanceOf(NamedTableReference.class, select.from());
    assertEquals("employees", ref.name());
    assertEquals("e", ref.alias());
  }

  @Test
  void parseSelectWithWhereClause() {
    ParseResult result =
        PARSER.parse(
            "SELECT id FROM employees WHERE id = 1", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertNotNull(select.where());
  }

  @Test
  void parseSelectWithOrderBy() {
    ParseResult result =
        PARSER.parse(
            "SELECT id FROM employees ORDER BY id DESC", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SelectStatement select = assertInstanceOf(SelectStatement.class, success.root());
    assertEquals(1, select.orderBy().size());
    assertEquals(io.github.e4c5.sqool.ast.SortDirection.DESC, select.orderBy().get(0).direction());
  }

  @Test
  void parseSelectDistinct() {
    ParseResult result =
        PARSER.parse(
            "SELECT DISTINCT name FROM employees", ParseOptions.defaults(SqlDialect.ORACLE));
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
            "INSERT INTO employees (name) VALUES ('Alice')",
            ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(InsertStatement.class, success.root());
  }

  @Test
  void parseUpdateProducesNormalizedAst() {
    ParseResult result =
        PARSER.parse(
            "UPDATE employees SET name = 'Bob' WHERE id = 1",
            ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(UpdateStatement.class, success.root());
  }

  @Test
  void parseDeleteProducesNormalizedAst() {
    ParseResult result =
        PARSER.parse(
            "DELETE FROM employees WHERE id = 1", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(DeleteStatement.class, success.root());
  }

  @Test
  void parseCreateTableAsRaw() {
    ParseResult result =
        PARSER.parse(
            "CREATE TABLE employees (id NUMBER(10) PRIMARY KEY, name VARCHAR2(100) NOT NULL)",
            ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    OracleRawStatement raw = assertInstanceOf(OracleRawStatement.class, success.root());
    assertEquals(OracleStatementKind.CREATE_TABLE, raw.kind());
    assertTrue(raw.sqlText().contains("CREATE"));
  }

  @Test
  void parseDropTableAsRaw() {
    ParseResult result =
        PARSER.parse("DROP TABLE employees", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    OracleRawStatement raw = assertInstanceOf(OracleRawStatement.class, success.root());
    assertEquals(OracleStatementKind.DROP_TABLE, raw.kind());
  }

  @Test
  void parseDropTableCascadeConstraintsAsRaw() {
    ParseResult result =
        PARSER.parse(
            "DROP TABLE employees CASCADE CONSTRAINTS PURGE",
            ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    OracleRawStatement raw = assertInstanceOf(OracleRawStatement.class, success.root());
    assertEquals(OracleStatementKind.DROP_TABLE, raw.kind());
  }

  // =========================================================================
  // SELECT fallback for complex shapes
  // =========================================================================

  @Test
  void parseSelectWithJoinProducesSelectStatement() {
    ParseResult result =
        PARSER.parse(
            "SELECT e.id, d.name FROM employees e"
                + " INNER JOIN departments d ON e.dept_id = d.id",
            ParseOptions.defaults(SqlDialect.ORACLE));
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
            "WITH cte AS (SELECT 1 FROM DUAL) SELECT * FROM cte",
            ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(OracleRawStatement.class, success.root());
  }

  @Test
  void parseSelectUnionAsRaw() {
    ParseResult result =
        PARSER.parse(
            "SELECT 1 FROM DUAL UNION SELECT 2 FROM DUAL",
            ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    assertInstanceOf(OracleRawStatement.class, success.root());
  }

  @Test
  void parseSelectWithFetchFirstAsRaw() {
    ParseResult result =
        PARSER.parse(
            "SELECT id FROM employees ORDER BY id FETCH FIRST 10 ROWS ONLY",
            ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    // FETCH FIRST is not yet in normalized AST; falls back to raw
    assertInstanceOf(OracleRawStatement.class, success.root());
  }

  // =========================================================================
  // Script mode
  // =========================================================================

  @Test
  void parseScriptMode() {
    String sql = "SELECT 1 FROM DUAL;\nSELECT 2 FROM DUAL;";
    ParseResult result =
        PARSER.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE).withScriptMode(true));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SqlScript script = assertInstanceOf(SqlScript.class, success.root());
    assertEquals(2, script.statements().size());
  }

  @Test
  void parseScriptModeWithMixedStatements() {
    String sql =
        "CREATE TABLE t (id NUMBER(10));\n" + "INSERT INTO t VALUES (1);\n" + "SELECT * FROM t;\n";
    ParseResult result =
        PARSER.parse(sql, ParseOptions.defaults(SqlDialect.ORACLE).withScriptMode(true));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    SqlScript script = assertInstanceOf(SqlScript.class, success.root());
    assertEquals(3, script.statements().size());
  }

  // =========================================================================
  // Validation tests
  // =========================================================================

  @Test
  void rejectsNullSql() {
    ParseResult result = PARSER.parse(null, ParseOptions.defaults(SqlDialect.ORACLE));
    assertInstanceOf(ParseFailure.class, result);
  }

  @Test
  void rejectsBlankSql() {
    ParseResult result = PARSER.parse("  ", ParseOptions.defaults(SqlDialect.ORACLE));
    assertInstanceOf(ParseFailure.class, result);
  }

  @Test
  void rejectsWrongDialect() {
    ParseResult result =
        PARSER.parse("SELECT 1 FROM DUAL", ParseOptions.defaults(SqlDialect.MYSQL));
    assertInstanceOf(ParseFailure.class, result);
  }

  @Test
  void rejectsInvalidSyntax() {
    ParseResult result = PARSER.parse("SELECT FROM", ParseOptions.defaults(SqlDialect.ORACLE));
    assertInstanceOf(ParseFailure.class, result);
  }

  @Test
  void selectColumnWithDotNotation() {
    ParseResult result =
        PARSER.parse("SELECT e.name FROM employees e", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    Statement stmt = assertInstanceOf(Statement.class, success.root());
    assertInstanceOf(SelectStatement.class, stmt);
    SelectStatement select = (SelectStatement) stmt;
    ExpressionSelectItem item =
        assertInstanceOf(ExpressionSelectItem.class, select.selectItems().get(0));
    IdentifierExpression ident = assertInstanceOf(IdentifierExpression.class, item.expression());
    assertEquals("e.name", ident.text());
  }

  @Test
  void parseCommitAsRaw() {
    ParseResult result = PARSER.parse("COMMIT", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    OracleRawStatement raw = assertInstanceOf(OracleRawStatement.class, success.root());
    assertEquals(OracleStatementKind.COMMIT, raw.kind());
  }

  @Test
  void parseRollbackAsRaw() {
    ParseResult result = PARSER.parse("ROLLBACK", ParseOptions.defaults(SqlDialect.ORACLE));
    ParseSuccess success = assertInstanceOf(ParseSuccess.class, result);
    OracleRawStatement raw = assertInstanceOf(OracleRawStatement.class, success.root());
    assertEquals(OracleStatementKind.ROLLBACK, raw.kind());
  }
}
