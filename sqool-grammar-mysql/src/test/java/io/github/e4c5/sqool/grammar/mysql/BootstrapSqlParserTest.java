package io.github.e4c5.sqool.grammar.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.e4c5.sqool.grammar.mysql.generated.BootstrapSqlLexer;
import io.github.e4c5.sqool.grammar.mysql.generated.BootstrapSqlParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

class BootstrapSqlParserTest {

  @Test
  void parsesSimpleSelectStatement() {
    var parsed = parseScript("SELECT * FROM demo;");

    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.script());
  }

  @Test
  void parsesLowercaseKeywords() {
    var parsed = parseScript("select * from demo;");

    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.script());
  }

  @Test
  void requiresSemicolonBetweenStatements() {
    var parsed = parseScript("SELECT * FROM demo SELECT * FROM other;");

    assertEquals(1, parsed.parser().getNumberOfSyntaxErrors());
  }

  private ParsedScript parseScript(String sql) {
    var lexer = new BootstrapSqlLexer(CharStreams.fromString(sql));
    var parser = new BootstrapSqlParser(new CommonTokenStream(lexer));
    parser.setBuildParseTree(false);
    return new ParsedScript(parser, parser.script());
  }

  private record ParsedScript(BootstrapSqlParser parser, BootstrapSqlParser.ScriptContext script) {}
}
