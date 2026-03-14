package io.github.e4c5.sqool.dialect.sqlite;

import io.github.e4c5.sqool.core.AntlrSyntaxErrorListener;
import io.github.e4c5.sqool.core.DiagnosticSeverity;
import io.github.e4c5.sqool.core.ParseAttempt;
import io.github.e4c5.sqool.core.ParseFailure;
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

    ParseAttempt<SQLiteParser.ParseContext> attempt =
        parseRoot(sql, effectiveOptions.enableFallback());
    if (!attempt.diagnostics().isEmpty()) {
      return new ParseFailure(SqlDialect.SQLITE, attempt.diagnostics());
    }

    SQLiteParser.Sql_stmt_listContext stmtList = attempt.context().sql_stmt_list();
    if (effectiveOptions.scriptMode()) {
      return SqliteAstMapper.mapSqlStmtList(stmtList, effectiveOptions);
    }

    List<SQLiteParser.Sql_stmtContext> stmts = stmtList.sql_stmt();
    if (stmts.isEmpty()) {
      return failure("No statement in input.", 1, 0, null);
    }
    if (stmts.size() > 1) {
      return failure("Multiple statements not allowed in single-statement mode.", 1, 0, null);
    }
    return SqliteAstMapper.mapSqlStmt(stmts.get(0), effectiveOptions);
  }

  private ParseAttempt<SQLiteParser.ParseContext> parseRoot(String sql, boolean enableFallback) {
    try {
      return parseRoot(sql, PredictionMode.SLL, new BailErrorStrategy());
    } catch (ParseCancellationException | InputMismatchException exception) {
      if (!enableFallback) {
        return ParseAttempt.failure(
            List.of(
                new SyntaxDiagnostic(
                    DiagnosticSeverity.ERROR, "Fast-path SQLite parse failed.", 1, 0, null)));
      }
      return parseRoot(sql, PredictionMode.LL, new DefaultErrorStrategy());
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
            new SyntaxDiagnostic(
                DiagnosticSeverity.ERROR, message, line, column, offendingToken)));
  }
}
