package io.github.e4c5.sqool.grammar.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.e4c5.sqool.grammar.sqlite.generated.SQLiteLexer;
import io.github.e4c5.sqool.grammar.sqlite.generated.SQLiteParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.junit.jupiter.api.Test;

class SQLiteGrammarSmokeTest {

  @Test
  void parsesSimpleSelectStatement() {
    var parsed = parse("SELECT 1");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesSelectStarFromTable() {
    var parsed = parse("SELECT * FROM t");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesCreateTable() {
    var parsed = parse("CREATE TABLE x (a INT);");
    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  private Parsed parse(String sql) {
    var lexer = new SQLiteLexer(CharStreams.fromString(sql));
    var parser = new SQLiteParser(new CommonTokenStream(lexer));
    parser.setBuildParseTree(true);
    SQLiteParser.ParseContext context = parser.parse();
    assertEquals(
        Token.EOF,
        parser.getCurrentToken().getType(),
        "Parser did not consume the full SQL input");
    return new Parsed(parser, context);
  }

  private record Parsed(SQLiteParser parser, SQLiteParser.ParseContext context) {}
}
