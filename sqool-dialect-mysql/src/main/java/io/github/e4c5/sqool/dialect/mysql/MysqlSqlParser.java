package io.github.e4c5.sqool.dialect.mysql;

import io.github.e4c5.sqool.core.DiagnosticSeverity;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.core.SqlParser;
import io.github.e4c5.sqool.core.SyntaxDiagnostic;
import io.github.e4c5.sqool.grammar.mysql.generated.MySQLLexer;
import io.github.e4c5.sqool.grammar.mysql.generated.MySQLParser;
import java.util.List;
import java.util.Objects;
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
    Objects.requireNonNull(sql, "sql");
    Objects.requireNonNull(options, "options");

    if (options.dialect() != SqlDialect.MYSQL) {
      return failure("MysqlSqlParser only accepts MYSQL parse options.", 1, 0, null);
    }
    if (options.scriptMode()) {
      var attempt = parseQueries(sql, options.enableFallback());
      if (!attempt.diagnostics().isEmpty()) {
        return new ParseFailure(SqlDialect.MYSQL, attempt.diagnostics());
      }
      return MysqlAstMapper.mapQueries(attempt.context(), options);
    }

    var attempt = parseSimpleStatement(sql, options.enableFallback());
    if (!attempt.diagnostics().isEmpty()) {
      return new ParseFailure(SqlDialect.MYSQL, attempt.diagnostics());
    }

    return MysqlAstMapper.mapSimpleStatement(attempt.context(), options);
  }

  private SimpleStatementAttempt parseSimpleStatement(String sql, boolean enableFallback) {
    try {
      return parseSimpleStatement(sql, PredictionMode.SLL, new BailErrorStrategy());
    } catch (ParseCancellationException | InputMismatchException exception) {
      if (!enableFallback) {
        return failureSimpleStatementAttempt(
            List.of(
                new SyntaxDiagnostic(
                    DiagnosticSeverity.ERROR, "Fast-path MySQL parse failed.", 1, 0, null)));
      }
      return parseSimpleStatement(sql, PredictionMode.LL, new DefaultErrorStrategy());
    }
  }

  private SimpleStatementAttempt parseSimpleStatement(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    var syntaxErrors = new MysqlSyntaxErrorListener();
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

    var context = parser.simpleStatement();
    requireEndOfInput(tokens, syntaxErrors);
    return syntaxErrors.hasDiagnostics()
        ? failureSimpleStatementAttempt(syntaxErrors.diagnostics())
        : new SimpleStatementAttempt(context, List.of());
  }

  private QueriesAttempt parseQueries(String sql, boolean enableFallback) {
    try {
      return parseQueries(sql, PredictionMode.SLL, new BailErrorStrategy());
    } catch (ParseCancellationException | InputMismatchException exception) {
      if (!enableFallback) {
        return failureQueriesAttempt(
            List.of(
                new SyntaxDiagnostic(
                    DiagnosticSeverity.ERROR, "Fast-path MySQL script parse failed.", 1, 0, null)));
      }
      return parseQueries(sql, PredictionMode.LL, new DefaultErrorStrategy());
    }
  }

  private QueriesAttempt parseQueries(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    var syntaxErrors = new MysqlSyntaxErrorListener();
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

    var context = parser.queries();
    return syntaxErrors.hasDiagnostics()
        ? failureQueriesAttempt(syntaxErrors.diagnostics())
        : new QueriesAttempt(context, List.of());
  }

  private void requireEndOfInput(CommonTokenStream tokens, MysqlSyntaxErrorListener syntaxErrors) {
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
          "Unexpected trailing tokens after SELECT statement.",
          null);
    }
  }

  private static ParseFailure failure(String message, int line, int column, String offendingToken) {
    return new ParseFailure(
        SqlDialect.MYSQL,
        List.of(
            new SyntaxDiagnostic(DiagnosticSeverity.ERROR, message, line, column, offendingToken)));
  }

  private static SimpleStatementAttempt failureSimpleStatementAttempt(
      List<SyntaxDiagnostic> diagnostics) {
    return new SimpleStatementAttempt(null, diagnostics);
  }

  private static QueriesAttempt failureQueriesAttempt(List<SyntaxDiagnostic> diagnostics) {
    return new QueriesAttempt(null, diagnostics);
  }

  private record SimpleStatementAttempt(
      MySQLParser.SimpleStatementContext context, List<SyntaxDiagnostic> diagnostics) {}

  private record QueriesAttempt(
      MySQLParser.QueriesContext context, List<SyntaxDiagnostic> diagnostics) {}
}
