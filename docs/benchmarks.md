# Parser Benchmarks

The `sqool-bench` module provides JMH benchmarks comparing `sqool` dialect parsers against JSqlParser for MySQL, SQLite, PostgreSQL, and Oracle.

## Running benchmarks

```bash
./gradlew :sqool-bench:jmh
```

To run a subset of benchmarks (e.g. PostgreSQL or Oracle only):

```bash
./gradlew :sqool-bench:jmh -Pjmh.includes=".*PostgresqlParserBenchmark.*"
./gradlew :sqool-bench:jmh -Pjmh.includes=".*OracleParserBenchmark.*"
```

## Capturing baseline results

To record baseline metrics in a reproducible form:

```bash
./gradlew :sqool-bench:jmh -Pjmh.rf=json -Pjmh.rff=build/reports/jmh/results.json
```

The JSON output contains one entry per benchmark method with the following key fields:

| Field | Description |
|-------|-------------|
| `benchmark` | Fully qualified benchmark method name (e.g. `io.github.e4c5.sqool.bench.MysqlParserBenchmark.parseWithSqool`) |
| `mode` | Benchmark mode (e.g. `thrpt` for throughput) |
| `primaryMetric.score` | Result value (e.g. ops/ms for throughput) |
| `primaryMetric.scoreUnit` | Unit of measurement |
| `primaryMetric.scoreConfidence` | 99% confidence interval `[low, high]` |
| `params` | Benchmark parameters (if any) |

For CI, benchmark results are stored as workflow artifacts under the name `jmh-results` on `main` branch runs. See the `.github/workflows/ci.yml` `benchmark` job. Artifacts are retained for 90 days.

## Comparing across dialects

Each dialect has its own benchmark class:

- `MysqlParserBenchmark` – MySQL
- `SqliteParserBenchmark` – SQLite
- `PostgresqlParserBenchmark` – PostgreSQL
- `OracleParserBenchmark` – Oracle (v1 subset: simple/join/complex/error-path; vs JSqlParser where supported)

To compare dialects, run all benchmarks and filter the JSON output by dialect name. For example, to extract throughput for the `parseWithSqool` method per dialect:

```bash
./gradlew :sqool-bench:jmh -Pjmh.rf=json -Pjmh.rff=/tmp/results.json
# Then inspect scores per dialect:
python3 -c "
import json, sys
data = json.load(open('/tmp/results.json'))
for b in data:
    print(b['benchmark'], b['primaryMetric']['score'], b['primaryMetric']['scoreUnit'])
"
```

Benchmark categories vary by dialect. PostgreSQL and Oracle include simple/join/complex/error-path scenarios; MySQL and SQLite currently cover a smaller subset. Run all benchmarks to compare currently implemented scenarios across dialects:

```bash
./gradlew :sqool-bench:jmh
```

## Corpus categories

| Category | Description |
|----------|-------------|
| Simple | Basic SELECT; exercises SLL fast path |
| Join | SELECT with INNER JOIN, WHERE, ORDER BY, LIMIT |
| Complex | Multiple joins, subqueries, aggregates, GROUP BY, HAVING |
| Error-path | Invalid SQL; measures diagnostics overhead |

## Interpreting results

- **Higher is better** for `thrpt` (throughput) mode, measured in ops/ms.
- A wide confidence interval indicates high variance; consider re-running with more iterations (`-Pjmh.i=5 -Pjmh.wi=3`).
- Compare `parseWithSqool` vs `parseWithJSqlParser` for the same dialect to assess relative performance.
- The SLL fast path (simple queries) should show higher throughput than LL-fallback path (complex or invalid queries).
- Cross-dialect throughput differences reflect grammar complexity and AST mapping cost, not just parsing overhead.
