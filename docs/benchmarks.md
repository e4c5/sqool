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

The JSON output can be compared across runs and across dialects. For CI, benchmark results are typically stored as artifacts (see `CONTRIBUTING.md`).

## Comparing across dialects

Each dialect has its own benchmark class:

- `MysqlParserBenchmark` – MySQL
- `SqliteParserBenchmark` – SQLite
- `PostgresqlParserBenchmark` – PostgreSQL
- `OracleParserBenchmark` – Oracle (v1 subset: simple/join/complex/error-path; vs JSqlParser where supported)

Benchmark categories vary by dialect today. PostgreSQL and Oracle include simple/join/complex/error-path scenarios; MySQL and SQLite currently cover a smaller subset. Run all benchmarks to compare currently implemented scenarios across dialects:

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
