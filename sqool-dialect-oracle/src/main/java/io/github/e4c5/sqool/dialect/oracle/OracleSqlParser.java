package io.github.e4c5.sqool.dialect.oracle;

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
import io.github.e4c5.sqool.grammar.oracle.generated.OracleLexer;
import io.github.e4c5.sqool.grammar.oracle.generated.OracleParser;
import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/** Oracle SQL parser facade implementing the shared {@link SqlParser} contract. */
public final class OracleSqlParser implements SqlParser {

  @Override
  public ParseResult parse(String sql, ParseOptions options) {
    if (sql == null || sql.isBlank()) {
      return failure("SQL input must not be null or blank.", 1, 0, null);
    }
    ParseOptions effectiveOptions =
        options == null ? ParseOptions.defaults(SqlDialect.ORACLE) : options;

    if (effectiveOptions.dialect() != SqlDialect.ORACLE) {
      return failure("OracleSqlParser only accepts ORACLE parse options.", 1, 0, null);
    }

    long startNanos = System.nanoTime();
    if (effectiveOptions.scriptMode()) {
      ParseOutcome<OracleParser.RootContext> outcome =
          parseRootWithMode(sql, effectiveOptions.enableFallback());
      ParseMetrics metrics =
          ParseMetrics.of(
              outcome.usedSll() ? ParseMetrics.PredictionMode.SLL : ParseMetrics.PredictionMode.LL,
              System.nanoTime() - startNanos);
      if (!outcome.attempt().diagnostics().isEmpty()) {
        return new ParseFailure(SqlDialect.ORACLE, outcome.attempt().diagnostics(), metrics);
      }
      return OracleAstMapper.mapRoot(outcome.attempt().context(), effectiveOptions)
          .withMetrics(metrics);
    }

    ParseOutcome<OracleParser.SingleStatementContext> outcome =
        parseSingleStatementWithMode(sql, effectiveOptions.enableFallback());
    ParseMetrics metrics =
        ParseMetrics.of(
            outcome.usedSll() ? ParseMetrics.PredictionMode.SLL : ParseMetrics.PredictionMode.LL,
            System.nanoTime() - startNanos);
    if (!outcome.attempt().diagnostics().isEmpty()) {
      return new ParseFailure(SqlDialect.ORACLE, outcome.attempt().diagnostics(), metrics);
    }
    return OracleAstMapper.mapSingleStatement(outcome.attempt().context(), effectiveOptions)
        .withMetrics(metrics);
  }

  private record ParseOutcome<C>(ParseAttempt<C> attempt, boolean usedSll) {}

  private ParseOutcome<OracleParser.RootContext> parseRootWithMode(
      String sql, boolean enableFallback) {
    try {
      return new ParseOutcome<>(parseRoot(sql, PredictionMode.SLL, new BailErrorStrategy()), true);
    } catch (ParseCancellationException | InputMismatchException exception) {
      ParseAttempt<OracleParser.RootContext> llAttempt =
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
                        "Fast-path Oracle script parse failed.",
                        1,
                        0,
                        null))),
            false);
      }
      return new ParseOutcome<>(llAttempt, false);
    }
  }

  private ParseAttempt<OracleParser.RootContext> parseRoot(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    return doParse(sql, predictionMode, errorStrategy, OracleParser::root);
  }

  private ParseOutcome<OracleParser.SingleStatementContext> parseSingleStatementWithMode(
      String sql, boolean enableFallback) {
    try {
      return new ParseOutcome<>(
          parseSingleStatement(sql, PredictionMode.SLL, new BailErrorStrategy()), true);
    } catch (ParseCancellationException | InputMismatchException exception) {
      ParseAttempt<OracleParser.SingleStatementContext> llAttempt =
          parseSingleStatement(sql, PredictionMode.LL, new DefaultErrorStrategy());
      if (!enableFallback) {
        if (!llAttempt.diagnostics().isEmpty()) {
          return new ParseOutcome<>(ParseAttempt.failure(llAttempt.diagnostics()), false);
        }
        return new ParseOutcome<>(
            ParseAttempt.failure(
                List.of(
                    new SyntaxDiagnostic(
                        DiagnosticSeverity.ERROR, "Fast-path Oracle parse failed.", 1, 0, null))),
            false);
      }
      return new ParseOutcome<>(llAttempt, false);
    }
  }

  private ParseAttempt<OracleParser.SingleStatementContext> parseSingleStatement(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    return doParse(sql, predictionMode, errorStrategy, OracleParser::singleStatement);
  }

  private <T> ParseAttempt<T> doParse(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy,
      Function<OracleParser, T> entryPoint) {
    AntlrSyntaxErrorListener syntaxErrors = new AntlrSyntaxErrorListener();
    var lexer = new OracleLexer(CharStreams.fromString(sql));
    lexer.removeErrorListeners();
    lexer.addErrorListener(syntaxErrors);

    var tokens = new CommonTokenStream(lexer);
    var parser = new OracleParser(tokens);
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
        SqlDialect.ORACLE,
        List.of(
            new SyntaxDiagnostic(DiagnosticSeverity.ERROR, message, line, column, offendingToken)),
        ParseMetrics.unknown());
  }
}
