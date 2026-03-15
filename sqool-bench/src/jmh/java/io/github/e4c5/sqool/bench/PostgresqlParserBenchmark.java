package io.github.e4c5.sqool.bench;

import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.dialect.postgresql.PostgresqlSqlParser;
import java.util.concurrent.TimeUnit;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/** JMH benchmarks for the PostgreSQL parser. Provides baseline metrics for the v1 subset. */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class PostgresqlParserBenchmark {

  private final PostgresqlSqlParser parser = new PostgresqlSqlParser();
  private final ParseOptions options = ParseOptions.defaults(SqlDialect.POSTGRESQL);

  // Representative application query exercising joins, predicates, ordering and pagination.
  private final String joinQuery =
      "SELECT u.id AS user_id, u.name FROM users u"
          + " INNER JOIN orders o ON u.id = o.user_id"
          + " WHERE o.total >= 100"
          + " ORDER BY o.created_at DESC"
          + " LIMIT 10 OFFSET 5";

  // Complex query within the v1 normalized subset: single table, predicates, GROUP BY, ORDER BY,
  // LIMIT. No JOINs, subqueries, aggregate function expressions, or OFFSET, which are outside the
  // subset and would trigger a raw-statement fallback.
  private final String complexQuery =
      "SELECT id, name FROM users"
          + " WHERE created_at >= '2024-01-01' AND id > 0"
          + " GROUP BY id, name"
          + " ORDER BY id DESC"
          + " LIMIT 20";

  // Simple SELECT-literal query that should always take the SLL fast path.
  private final String simpleQuery = "SELECT id, name, email FROM users WHERE id = 1";

  // Error-path query for diagnostics overhead measurement.
  private final String invalidQuery = "SELECT FROM";

  @Benchmark
  public ParseResult parseJoinQueryWithSqool() {
    return parser.parse(joinQuery, options);
  }

  @Benchmark
  public Object parseJoinQueryWithJSqlParser() throws Exception {
    return CCJSqlParserUtil.parse(joinQuery);
  }

  @Benchmark
  public ParseResult parseSimpleQueryWithSqool() {
    return parser.parse(simpleQuery, options);
  }

  @Benchmark
  public Object parseSimpleQueryWithJSqlParser() throws Exception {
    return CCJSqlParserUtil.parse(simpleQuery);
  }

  @Benchmark
  public ParseResult parseComplexQueryWithSqool() {
    return parser.parse(complexQuery, options);
  }

  @Benchmark
  public Object parseComplexQueryWithJSqlParser() throws Exception {
    return CCJSqlParserUtil.parse(complexQuery);
  }

  @Benchmark
  public ParseResult parseErrorPathWithSqool() {
    return parser.parse(invalidQuery, options);
  }
}
