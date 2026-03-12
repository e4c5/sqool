package io.github.e4c5.sqool.grammar.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.e4c5.sqool.grammar.mysql.generated.MySQLLexer;
import io.github.e4c5.sqool.grammar.mysql.generated.MySQLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

class MySQLGrammarSmokeTest {

  @Test
  void parsesSimpleSelectStatement() {
    var parsed = parseSimpleStatement("SELECT id, name FROM demo");

    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesLowercaseSelectStatement() {
    var parsed = parseSimpleStatement("select * from demo");

    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  private ParsedStatement parseSimpleStatement(String sql) {
    var lexer = new MySQLLexer(CharStreams.fromString(sql));
    var parser = new MySQLParser(new CommonTokenStream(lexer));
    parser.setBuildParseTree(true);
    return new ParsedStatement(parser, parser.simpleStatement());
  }

  private record ParsedStatement(MySQLParser parser, MySQLParser.SimpleStatementContext context) {}
}
