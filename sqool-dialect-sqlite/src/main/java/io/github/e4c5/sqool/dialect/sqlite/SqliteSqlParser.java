package io.github.e4c5.sqool.dialect.sqlite;

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
import io.github.e4c5.sqool.grammar.sqlite.generated.SQLiteLexer;
import io.github.e4c5.sqool.grammar.sqlite.generated.SQLiteParser;
import java.util.List;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/** Minimal SQLite parser facade for Phase 2 (SQLite MVP). */
public final class SqliteSqlParser implements SqlParser {

  @Override
  public ParseResult parse(String sql, ParseOptions options) {
    if (sql == null || sql.isBlank()) {
      return failure("SQL input must not be null or blank.", 1, 0, null);
    }
    ParseOptions effectiveOptions =
        options == null ? ParseOptions.defaults(SqlDialect.SQLITE) : options;

    if (effectiveOptions.dialect() != SqlDialect.SQLITE) {
      return failure("SqliteSqlParser only accepts SQLITE parse options.", 1, 0, null);
    }

    long startNanos = System.nanoTime();
    ParseOutcome<SQLiteParser.ParseContext> outcome =
        parseRootWithMode(sql, effectiveOptions.enableFallback());
    ParseMetrics metrics =
        ParseMetrics.of(
            outcome.usedSll() ? ParseMetrics.PredictionMode.SLL : ParseMetrics.PredictionMode.LL,
            System.nanoTime() - startNanos);

    if (!outcome.attempt().diagnostics().isEmpty()) {
      return new ParseFailure(SqlDialect.SQLITE, outcome.attempt().diagnostics(), metrics);
    }

    SQLiteParser.Sql_stmt_listContext stmtList = outcome.attempt().context().sql_stmt_list();
    if (effectiveOptions.scriptMode()) {
      return SqliteAstMapper.mapSqlStmtList(stmtList, effectiveOptions).withMetrics(metrics);
    }

    List<SQLiteParser.Sql_stmtContext> stmts = stmtList.sql_stmt();
    if (stmts.isEmpty()) {
      return failure("No statement in input.", 1, 0, null);
    }
    if (stmts.size() > 1) {
      return failure("Multiple statements not allowed in single-statement mode.", 1, 0, null);
    }
    return SqliteAstMapper.mapSqlStmt(stmts.get(0), effectiveOptions).withMetrics(metrics);
  }

  private record ParseOutcome<C>(ParseAttempt<C> attempt, boolean usedSll) {}

  private ParseOutcome<SQLiteParser.ParseContext> parseRootWithMode(
      String sql, boolean enableFallback) {
    try {
      return new ParseOutcome<>(parseRoot(sql, PredictionMode.SLL, new BailErrorStrategy()), true);
    } catch (ParseCancellationException | InputMismatchException exception) {
      // SLL fast-path failed; run an LL pass to surface real syntax diagnostics.
      ParseAttempt<SQLiteParser.ParseContext> llAttempt =
          parseRoot(sql, PredictionMode.LL, new DefaultErrorStrategy());
      if (!enableFallback) {
        if (!llAttempt.diagnostics().isEmpty()) {
          return new ParseOutcome<>(ParseAttempt.failure(llAttempt.diagnostics()), false);
        }
        return new ParseOutcome<>(
            ParseAttempt.failure(
                List.of(
                    new SyntaxDiagnostic(
                        DiagnosticSeverity.ERROR, "Fast-path SQLite parse failed.", 1, 0, null))),
            false);
      }
      return new ParseOutcome<>(llAttempt, false);
    }
  }

  private ParseAttempt<SQLiteParser.ParseContext> parseRoot(
      String sql,
      PredictionMode predictionMode,
      org.antlr.v4.runtime.ANTLRErrorStrategy errorStrategy) {
    AntlrSyntaxErrorListener syntaxErrors = new AntlrSyntaxErrorListener();
    var lexer = new SQLiteLexer(CharStreams.fromString(sql));
    lexer.removeErrorListeners();
    lexer.addErrorListener(syntaxErrors);

    var tokens = new CommonTokenStream(lexer);
    var parser = new SQLiteParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(syntaxErrors);
    parser.setBuildParseTree(true);
    parser.setErrorHandler(errorStrategy);
    parser.getInterpreter().setPredictionMode(predictionMode);

    SQLiteParser.ParseContext context = parser.parse();
    return syntaxErrors.hasDiagnostics()
        ? ParseAttempt.failure(syntaxErrors.diagnostics())
        : new ParseAttempt<>(context, List.of());
  }

  private static ParseFailure failure(String message, int line, int column, String offendingToken) {
    return new ParseFailure(
        SqlDialect.SQLITE,
        List.of(
            new SyntaxDiagnostic(DiagnosticSeverity.ERROR, message, line, column, offendingToken)),
        ParseMetrics.unknown());
  }
}
