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
    var lexer = new BootstrapSqlLexer(CharStreams.fromString("SELECT * FROM demo;"));
    var parser = new BootstrapSqlParser(new CommonTokenStream(lexer));
    parser.setBuildParseTree(false);

    var script = parser.script();

    assertEquals(0, parser.getNumberOfSyntaxErrors());
    assertNotNull(script);
  }
}
