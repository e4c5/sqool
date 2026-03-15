package io.github.e4c5.sqool.grammar.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.e4c5.sqool.grammar.oracle.generated.OracleLexer;
import io.github.e4c5.sqool.grammar.oracle.generated.OracleParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

class OracleGrammarSmokeTest {

  @Test
  void parsesSimpleSelectLiteral() {
    var parsed = parse("SELECT 1 FROM DUAL");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesSelectStarFromTable() {
    var parsed = parse("SELECT * FROM employees");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesCreateTable() {
    var parsed =
        parse(
            "CREATE TABLE employees (id NUMBER(10) PRIMARY KEY,"
                + " name VARCHAR2(100) NOT NULL,"
                + " hire_date DATE)");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesInsertStatement() {
    var parsed = parse("INSERT INTO employees (name) VALUES ('Alice')");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesUpdateStatement() {
    var parsed = parse("UPDATE employees SET name = 'Bob' WHERE id = 1");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesDeleteStatement() {
    var parsed = parse("DELETE FROM employees WHERE id = 1");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesDropTable() {
    var parsed = parse("DROP TABLE employees");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesSelectWithJoin() {
    var parsed =
        parse(
            "SELECT e.id, d.name FROM employees e"
                + " INNER JOIN departments d ON e.dept_id = d.id"
                + " WHERE d.name = 'Engineering'");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesSelectWithOrderBy() {
    var parsed = parse("SELECT id, name FROM employees ORDER BY name ASC");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesSelectWithFetchFirst() {
    var parsed = parse("SELECT id, name FROM employees ORDER BY id FETCH FIRST 10 ROWS ONLY");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesDeleteWithoutFrom() {
    var parsed = parse("DELETE employees WHERE id = 1");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  private Parsed parse(String sql) {
    var lexer = new OracleLexer(CharStreams.fromString(sql));
    var parser = new OracleParser(new CommonTokenStream(lexer));
    parser.setBuildParseTree(true);
    OracleParser.SingleStatementContext context = parser.singleStatement();
    assertEquals(
        Token.EOF, parser.getCurrentToken().getType(), "Parser did not consume the full SQL input");
    return new Parsed(parser, context);
  }

  private record Parsed(OracleParser parser, OracleParser.SingleStatementContext context) {}
}
