package io.github.e4c5.sqool.dialect.postgresql;

import io.github.e4c5.sqool.core.AntlrSyntaxErrorListener;
import io.github.e4c5.sqool.core.DiagnosticSeverity;
import io.github.e4c5.sqool.core.ParseAttempt;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseMetrics;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.core.SqlParser;
import io.github.e4c5.sqool.core.SyntaxDiagnostic;
import io.github.e4c5.sqool.grammar.postgresql.generated.PostgreSQLLexer;
import io.github.e4c5.sqool.grammar.postgresql.generated.PostgreSQLParser;
import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/** PostgreSQL parser facade implementing the shared {@link SqlParser} contract. */
public final class PostgresqlSqlParser implements SqlParser {

  @Override
  public ParseResult parse(String sql, ParseOptions options) {
    if (sql == null || sql.isBlank()) {
      return failure("SQL input must not be null or blank.", 1, 0, null);
    }
    ParseOptions effectiveOptions =
        options == null ? ParseOptions.defaults(SqlDialect.POSTGRESQL) : options;

    if (effectiveOptions.dialect() != SqlDialect.POSTGRESQL) {
      return failure("PostgresqlSqlParser only accepts POSTGRESQL parse options.", 1, 0, null);
    }

    long startNanos = System.nanoTime();
    if (effectiveOptions.scriptMode()) {
      ParseOutcome<PostgreSQLParser.RootContext> outcome =
          parseRootWithMode(sql, effectiveOptions.enableFallback());
      ParseMetrics metrics =
          ParseMetrics.of(
              outcome.usedSll() ? ParseMetrics.PredictionMode.SLL : ParseMetrics.PredictionMode.LL,
              System.nanoTime() - startNanos);
      if (!outcome.attempt().diagnostics().isEmpty()) {
        return new ParseFailure(SqlDialect.POSTGRESQL, outcome.attempt().diagnostics(), metrics);
      }
      return PostgresqlAstMapper.mapRoot(outcome.attempt().context(), effectiveOptions)
          .withMetrics(metrics);
    }

    ParseOutcome<PostgreSQLParser.SingleStatementContext> outcome =
        parseSingleStatementWithMode(sql, effectiveOptions.enableFallback());
    ParseMetrics metrics =
        ParseMetrics.of(
            outcome.usedSll() ? ParseMetrics.PredictionMode.SLL : ParseMetrics.PredictionMode.LL,
            System.nanoTime() - startNanos);
    if (!outcome.attempt().diagnostics().isEmpty()) {
      return new ParseFailure(SqlDialect.POSTGRESQL, outcome.attempt().diagnostics(), metrics);
    }
    return PostgresqlAstMapper.mapSingleStatement(outcome.attempt().context(), effectiveOptions)
        .withMetrics(metrics);
  }

  private record ParseOutcome<C>(ParseAttempt<C> attempt, boolean usedSll) {}

  private ParseOutcome<PostgreSQLParser.RootContext> parseRootWithMode(
      String sql, boolean enableFallback) {
    try {
      return new ParseOutcome<>(parseRoot(sql, PredictionMode.SLL, new BailErrorStrategy()), true);
    } catch (ParseCancellationException | InputMismatchException exception) {
      ParseAttempt<PostgreSQLParser.RootContext> llAttempt =
          parseRoot(sql, PredictionMode.LL, new DefaultErrorStrategy());
      if (!enableFallback) {
        if (!llAttempt.diagnostics().isEmpty()) {
          return new ParseOutcome<>(ParseAttempt.failure(llAttempt.diagnostics()), false);
        }
        return new ParseOutcome<>(
            ParseAttempt.failure(
                List.of(
                    new SyntaxDiagnostic(
                        DiagnosticSeverity.ERROR,
                        "Fast-path PostgreSQL script parse failed.",
                        1,
                        0,
                        null))),
            false);
      }
      return new ParseOutcome<>(llAttempt, false);
    }
  }

  private ParseAttempt<PostgreSQLParser.RootContext> parseRoot(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    return doParse(sql, predictionMode, errorStrategy, PostgreSQLParser::root);
  }

  private ParseOutcome<PostgreSQLParser.SingleStatementContext> parseSingleStatementWithMode(
      String sql, boolean enableFallback) {
    try {
      return new ParseOutcome<>(
          parseSingleStatement(sql, PredictionMode.SLL, new BailErrorStrategy()), true);
    } catch (ParseCancellationException | InputMismatchException exception) {
      ParseAttempt<PostgreSQLParser.SingleStatementContext> llAttempt =
          parseSingleStatement(sql, PredictionMode.LL, new DefaultErrorStrategy());
      if (!enableFallback) {
        if (!llAttempt.diagnostics().isEmpty()) {
          return new ParseOutcome<>(ParseAttempt.failure(llAttempt.diagnostics()), false);
        }
        return new ParseOutcome<>(
            ParseAttempt.failure(
                List.of(
                    new SyntaxDiagnostic(
                        DiagnosticSeverity.ERROR,
                        "Fast-path PostgreSQL parse failed.",
                        1,
                        0,
                        null))),
            false);
      }
      return new ParseOutcome<>(llAttempt, false);
    }
  }

  private ParseAttempt<PostgreSQLParser.SingleStatementContext> parseSingleStatement(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    return doParse(sql, predictionMode, errorStrategy, PostgreSQLParser::singleStatement);
  }

  private <T> ParseAttempt<T> doParse(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy,
      Function<PostgreSQLParser, T> entryPoint) {
    AntlrSyntaxErrorListener syntaxErrors = new AntlrSyntaxErrorListener();
    var lexer = new PostgreSQLLexer(CharStreams.fromString(sql));
    lexer.removeErrorListeners();
    lexer.addErrorListener(syntaxErrors);

    var tokens = new CommonTokenStream(lexer);
    var parser = new PostgreSQLParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(syntaxErrors);
    parser.setBuildParseTree(true);
    parser.setErrorHandler(errorStrategy);
    parser.getInterpreter().setPredictionMode(predictionMode);

    T context = entryPoint.apply(parser);
    return syntaxErrors.hasDiagnostics()
        ? ParseAttempt.failure(syntaxErrors.diagnostics())
        : new ParseAttempt<>(context, List.of());
  }

  private static ParseFailure failure(String message, int line, int column, String offendingToken) {
    return new ParseFailure(
        SqlDialect.POSTGRESQL,
        List.of(
            new SyntaxDiagnostic(DiagnosticSeverity.ERROR, message, line, column, offendingToken)),
        ParseMetrics.unknown());
  }
}
