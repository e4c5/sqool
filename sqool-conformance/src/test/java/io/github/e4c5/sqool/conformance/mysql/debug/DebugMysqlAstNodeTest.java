package io.github.e4c5.sqool.conformance.mysql.debug;

import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.dialect.mysql.MysqlSqlParser;
import org.junit.jupiter.api.Test;

public class DebugMysqlAstNodeTest {
  @Test
  public void testAst() {
    MysqlSqlParser parser = new MysqlSqlParser();
    String sql =
        "create table if not exists users ( id bigint primary key auto_increment, name varchar(255) not null, created_at timestamp );";
    var result = parser.parse(sql, ParseOptions.defaults(SqlDialect.MYSQL).withScriptMode(true));
    System.out.println("Result: " + result);
  }
}
