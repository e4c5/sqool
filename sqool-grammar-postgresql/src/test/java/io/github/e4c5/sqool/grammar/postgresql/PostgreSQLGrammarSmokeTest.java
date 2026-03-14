package io.github.e4c5.sqool.grammar.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.e4c5.sqool.grammar.postgresql.generated.PostgreSQLLexer;
import io.github.e4c5.sqool.grammar.postgresql.generated.PostgreSQLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

class PostgreSQLGrammarSmokeTest {

  @Test
  void parsesSimpleSelectLiteral() {
    var parsed = parse("SELECT 1");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesSelectStarFromTable() {
    var parsed = parse("SELECT * FROM users");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesCreateTable() {
    var parsed = parse("CREATE TABLE users (id SERIAL PRIMARY KEY, name TEXT NOT NULL)");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesInsertStatement() {
    var parsed = parse("INSERT INTO users (name) VALUES ('Alice')");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesInsertReturning() {
    var parsed = parse("INSERT INTO users (name) VALUES ('Alice') RETURNING id");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesUpdateStatement() {
    var parsed = parse("UPDATE users SET name = 'Bob' WHERE id = 1");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesDeleteStatement() {
    var parsed = parse("DELETE FROM users WHERE id = 1");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesDropTable() {
    var parsed = parse("DROP TABLE IF EXISTS users CASCADE");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesSelectWithJoin() {
    var parsed =
        parse(
            "SELECT u.id, o.total FROM users u INNER JOIN orders o ON u.id = o.user_id"
                + " WHERE o.total > 100");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesSelectWithOrderByAndLimit() {
    var parsed = parse("SELECT id, name FROM users ORDER BY name ASC LIMIT 10 OFFSET 5");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  private Parsed parse(String sql) {
    var lexer = new PostgreSQLLexer(CharStreams.fromString(sql));
    var parser = new PostgreSQLParser(new CommonTokenStream(lexer));
    parser.setBuildParseTree(true);
    PostgreSQLParser.SingleStatementContext context = parser.singleStatement();
    assertEquals(
        Token.EOF, parser.getCurrentToken().getType(), "Parser did not consume the full SQL input");
    return new Parsed(parser, context);
  }

  private record Parsed(PostgreSQLParser parser, PostgreSQLParser.SingleStatementContext context) {}
}
