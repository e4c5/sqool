package io.github.e4c5.sqool.bench;

import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.dialect.oracle.OracleSqlParser;
import java.util.concurrent.TimeUnit;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/** JMH benchmarks for the Oracle SQL parser. Provides baseline metrics for the v1 subset. */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class OracleParserBenchmark {

  private final OracleSqlParser parser = new OracleSqlParser();
  private final ParseOptions options = ParseOptions.defaults(SqlDialect.ORACLE);

  // Representative application query exercising joins, predicates, and ordering.
  private final String joinQuery =
      "SELECT e.id AS emp_id, e.name, d.name AS dept_name"
          + " FROM employees e INNER JOIN departments d ON e.dept_id = d.id"
          + " WHERE e.salary >= 50000"
          + " ORDER BY e.name ASC";

  // Complex query within the v1 normalized subset: single table, predicates, GROUP BY, ORDER BY.
  private final String complexQuery =
      "SELECT department_id FROM employees"
          + " WHERE hire_date >= '2020-01-01' AND salary > 0"
          + " GROUP BY department_id"
          + " ORDER BY department_id ASC";

  // Simple SELECT query that should always take the SLL fast path.
  private final String simpleQuery = "SELECT id, name, salary FROM employees WHERE id = 1";

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
