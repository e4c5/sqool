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

  @Test
  void parsesJoinWhereOrderAndLimitStatement() {
    var parsed =
        parseSimpleStatement(
            "select u.id as user_id, u.name from users u "
                + "inner join orders o on u.id = o.user_id "
                + "where o.total >= 100 order by o.created_at desc limit 10 offset 5");

    assertEquals(0, parsed.parser().getNumberOfSyntaxErrors());
    assertNotNull(parsed.context());
  }

  @Test
  void parsesDistinctGroupedAggregateStatement() {
    var parsed =
        parseSimpleStatement(
            "select distinct category, count(*) as total "
                + "from sales where amount between 10 and 20 "
                + "group by category having count(*) > 1 "
                + "order by total desc limit 3");

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
