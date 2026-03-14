package io.github.e4c5.sqool.dialect.postgresql;

import io.github.e4c5.sqool.core.AntlrSyntaxErrorListener;
import io.github.e4c5.sqool.core.DiagnosticSeverity;
import io.github.e4c5.sqool.core.ParseAttempt;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.core.SqlParser;
import io.github.e4c5.sqool.core.SyntaxDiagnostic;
import io.github.e4c5.sqool.grammar.postgresql.generated.PostgreSQLLexer;
import io.github.e4c5.sqool.grammar.postgresql.generated.PostgreSQLParser;
import java.util.List;
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

    if (effectiveOptions.scriptMode()) {
      ParseAttempt<PostgreSQLParser.RootContext> attempt =
          parseRoot(sql, effectiveOptions.enableFallback());
      if (!attempt.diagnostics().isEmpty()) {
        return new ParseFailure(SqlDialect.POSTGRESQL, attempt.diagnostics());
      }
      return PostgresqlAstMapper.mapRoot(attempt.context(), effectiveOptions);
    }

    ParseAttempt<PostgreSQLParser.SingleStatementContext> attempt =
        parseSingleStatement(sql, effectiveOptions.enableFallback());
    if (!attempt.diagnostics().isEmpty()) {
      return new ParseFailure(SqlDialect.POSTGRESQL, attempt.diagnostics());
    }
    return PostgresqlAstMapper.mapSingleStatement(attempt.context(), effectiveOptions);
  }

  private ParseAttempt<PostgreSQLParser.RootContext> parseRoot(String sql, boolean enableFallback) {
    try {
      return parseRoot(sql, PredictionMode.SLL, new BailErrorStrategy());
    } catch (ParseCancellationException | InputMismatchException exception) {
      ParseAttempt<PostgreSQLParser.RootContext> llAttempt =
          parseRoot(sql, PredictionMode.LL, new DefaultErrorStrategy());
      if (!enableFallback) {
        if (!llAttempt.diagnostics().isEmpty()) {
          return ParseAttempt.failure(llAttempt.diagnostics());
        }
        return ParseAttempt.failure(
            List.of(
                new SyntaxDiagnostic(
                    DiagnosticSeverity.ERROR,
                    "Fast-path PostgreSQL script parse failed.",
                    1,
                    0,
                    null)));
      }
      return llAttempt;
    }
  }

  private ParseAttempt<PostgreSQLParser.RootContext> parseRoot(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
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

    PostgreSQLParser.RootContext context = parser.root();
    return syntaxErrors.hasDiagnostics()
        ? ParseAttempt.failure(syntaxErrors.diagnostics())
        : new ParseAttempt<>(context, List.of());
  }

  private ParseAttempt<PostgreSQLParser.SingleStatementContext> parseSingleStatement(
      String sql, boolean enableFallback) {
    try {
      return parseSingleStatement(sql, PredictionMode.SLL, new BailErrorStrategy());
    } catch (ParseCancellationException | InputMismatchException exception) {
      ParseAttempt<PostgreSQLParser.SingleStatementContext> llAttempt =
          parseSingleStatement(sql, PredictionMode.LL, new DefaultErrorStrategy());
      if (!enableFallback) {
        if (!llAttempt.diagnostics().isEmpty()) {
          return ParseAttempt.failure(llAttempt.diagnostics());
        }
        return ParseAttempt.failure(
            List.of(
                new SyntaxDiagnostic(
                    DiagnosticSeverity.ERROR, "Fast-path PostgreSQL parse failed.", 1, 0, null)));
      }
      return llAttempt;
    }
  }

  private ParseAttempt<PostgreSQLParser.SingleStatementContext> parseSingleStatement(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
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

    PostgreSQLParser.SingleStatementContext context = parser.singleStatement();
    return syntaxErrors.hasDiagnostics()
        ? ParseAttempt.failure(syntaxErrors.diagnostics())
        : new ParseAttempt<>(context, List.of());
  }

  private static ParseFailure failure(String message, int line, int column, String offendingToken) {
    return new ParseFailure(
        SqlDialect.POSTGRESQL,
        List.of(
            new SyntaxDiagnostic(DiagnosticSeverity.ERROR, message, line, column, offendingToken)));
  }
}
