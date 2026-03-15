package io.github.e4c5.sqool.dialect.mysql;

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
import io.github.e4c5.sqool.grammar.mysql.generated.MySQLLexer;
import io.github.e4c5.sqool.grammar.mysql.generated.MySQLParser;
import java.util.List;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/** Minimal MySQL parser facade for the first MVP dialect slice. */
public final class MysqlSqlParser implements SqlParser {

  @Override
  public ParseResult parse(String sql, ParseOptions options) {
    if (sql == null || sql.isBlank()) {
      return failure("SQL input must not be null or blank.", 1, 0, null);
    }
    ParseOptions effectiveOptions =
        options == null ? ParseOptions.defaults(SqlDialect.MYSQL) : options;

    if (effectiveOptions.dialect() != SqlDialect.MYSQL) {
      return failure("MysqlSqlParser only accepts MYSQL parse options.", 1, 0, null);
    }
    long startNanos = System.nanoTime();
    if (effectiveOptions.scriptMode()) {
      var outcome = parseQueriesWithMode(sql, effectiveOptions.enableFallback());
      ParseMetrics metrics =
          ParseMetrics.of(
              outcome.usedSll() ? ParseMetrics.PredictionMode.SLL : ParseMetrics.PredictionMode.LL,
              System.nanoTime() - startNanos);
      if (!outcome.attempt().diagnostics().isEmpty()) {
        return new ParseFailure(SqlDialect.MYSQL, outcome.attempt().diagnostics(), metrics);
      }
      try {
        return MysqlAstMapper.mapQueries(outcome.attempt().context(), effectiveOptions)
            .withMetrics(metrics);
      } catch (MysqlAstMapper.UnsupportedFeatureException exception) {
        return unsupportedFeatureFailure(exception, metrics);
      }
    }

    var outcome = parseSimpleStatementWithMode(sql, effectiveOptions.enableFallback());
    ParseMetrics metrics =
        ParseMetrics.of(
            outcome.usedSll() ? ParseMetrics.PredictionMode.SLL : ParseMetrics.PredictionMode.LL,
            System.nanoTime() - startNanos);
    if (!outcome.attempt().diagnostics().isEmpty()) {
      return new ParseFailure(SqlDialect.MYSQL, outcome.attempt().diagnostics(), metrics);
    }
    try {
      return MysqlAstMapper.mapSimpleStatement(outcome.attempt().context(), effectiveOptions)
          .withMetrics(metrics);
    } catch (MysqlAstMapper.UnsupportedFeatureException exception) {
      return unsupportedFeatureFailure(exception, metrics);
    }
  }

  private record ParseOutcome<C>(ParseAttempt<C> attempt, boolean usedSll) {}

  private ParseOutcome<MySQLParser.SimpleStatementContext> parseSimpleStatementWithMode(
      String sql, boolean enableFallback) {
    try {
      return new ParseOutcome<>(
          parseSimpleStatement(sql, PredictionMode.SLL, new BailErrorStrategy()), true);
    } catch (ParseCancellationException | InputMismatchException exception) {
      if (!enableFallback) {
        return new ParseOutcome<>(
            ParseAttempt.failure(
                List.of(
                    new SyntaxDiagnostic(
                        DiagnosticSeverity.ERROR, "Fast-path MySQL parse failed.", 1, 0, null))),
            false);
      }
      return new ParseOutcome<>(
          parseSimpleStatement(sql, PredictionMode.LL, new DefaultErrorStrategy()), false);
    }
  }

  private ParseAttempt<MySQLParser.SimpleStatementContext> parseSimpleStatement(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    return parseWith(
        sql,
        predictionMode,
        errorStrategy,
        (parser, tokens, syntaxErrors) -> {
          var context = parser.simpleStatement();
          requireEndOfInput(tokens, syntaxErrors);
          return context;
        });
  }

  private ParseOutcome<MySQLParser.QueriesContext> parseQueriesWithMode(
      String sql, boolean enableFallback) {
    try {
      return new ParseOutcome<>(
          parseQueries(sql, PredictionMode.SLL, new BailErrorStrategy()), true);
    } catch (ParseCancellationException | InputMismatchException exception) {
      if (!enableFallback) {
        return new ParseOutcome<>(
            ParseAttempt.failure(
                List.of(
                    new SyntaxDiagnostic(
                        DiagnosticSeverity.ERROR,
                        "Fast-path MySQL script parse failed.",
                        1,
                        0,
                        null))),
            false);
      }
      return new ParseOutcome<>(
          parseQueries(sql, PredictionMode.LL, new DefaultErrorStrategy()), false);
    }
  }

  private ParseAttempt<MySQLParser.QueriesContext> parseQueries(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    return parseWith(
        sql, predictionMode, errorStrategy, (parser, tokens, syntaxErrors) -> parser.queries());
  }

  private <C> ParseAttempt<C> parseWith(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy,
      ParserRunner<C> runner) {
    var syntaxErrors = new AntlrSyntaxErrorListener();
    var lexer = new MySQLLexer(CharStreams.fromString(sql));
    lexer.removeErrorListeners();
    lexer.addErrorListener(syntaxErrors);

    var tokens = new CommonTokenStream(lexer);
    var parser = new MySQLParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(syntaxErrors);
    parser.setBuildParseTree(true);
    parser.setErrorHandler(errorStrategy);
    parser.getInterpreter().setPredictionMode(predictionMode);

    var context = runner.run(parser, tokens, syntaxErrors);
    return syntaxErrors.hasDiagnostics()
        ? ParseAttempt.failure(syntaxErrors.diagnostics())
        : new ParseAttempt<>(context, List.of());
  }

  private interface ParserRunner<C> {
    C run(MySQLParser parser, CommonTokenStream tokens, AntlrSyntaxErrorListener syntaxErrors);
  }

  private void requireEndOfInput(CommonTokenStream tokens, AntlrSyntaxErrorListener syntaxErrors) {
    if (tokens.LA(1) == MySQLLexer.SEMICOLON_SYMBOL) {
      tokens.consume();
    }

    if (tokens.LA(1) != Token.EOF) {
      var trailingToken = tokens.LT(1);
      syntaxErrors.syntaxError(
          null,
          trailingToken,
          trailingToken.getLine(),
          trailingToken.getCharPositionInLine(),
          "Unexpected trailing tokens after statement.",
          null);
    }
  }

  private static ParseFailure unsupportedFeatureFailure(
      MysqlAstMapper.UnsupportedFeatureException exception, ParseMetrics metrics) {
    Token token = exception.token();
    return new ParseFailure(
        SqlDialect.MYSQL,
        List.of(
            new SyntaxDiagnostic(
                DiagnosticSeverity.ERROR,
                exception.getMessage(),
                token == null ? 1 : token.getLine(),
                token == null ? 0 : token.getCharPositionInLine(),
                token == null ? null : token.getText())),
        metrics);
  }

  private static ParseFailure failure(String message, int line, int column, String offendingToken) {
    return new ParseFailure(
        SqlDialect.MYSQL,
        List.of(
            new SyntaxDiagnostic(DiagnosticSeverity.ERROR, message, line, column, offendingToken)),
        ParseMetrics.unknown());
  }
}
