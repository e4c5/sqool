package io.github.e4c5.sqool.bench;

import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.dialect.mysql.MysqlSqlParser;
import java.util.concurrent.TimeUnit;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class MysqlParserBenchmark {
  private final MysqlSqlParser parser = new MysqlSqlParser();
  private final ParseOptions options = ParseOptions.defaults(SqlDialect.MYSQL);
  private final String query =
      "select u.id as user_id, u.name from users u "
          + "inner join orders o on u.id = o.user_id "
          + "where o.total >= 100 order by o.created_at desc limit 10 offset 5";

  @Benchmark
  public ParseResult parseWithSqool() {
    return parser.parse(query, options);
  }

  @Benchmark
  public Object parseWithJSqlParser() throws Exception {
    return CCJSqlParserUtil.parse(query);
  }
}
