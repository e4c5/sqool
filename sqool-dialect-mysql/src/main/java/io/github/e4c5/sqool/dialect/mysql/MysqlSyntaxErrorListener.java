package io.github.e4c5.sqool.dialect.mysql;

import io.github.e4c5.sqool.core.DiagnosticSeverity;
import io.github.e4c5.sqool.core.SyntaxDiagnostic;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

final class MysqlSyntaxErrorListener extends BaseErrorListener {
  private final List<SyntaxDiagnostic> diagnostics = new ArrayList<>();

  @Override
  public void syntaxError(
      Recognizer<?, ?> recognizer,
      Object offendingSymbol,
      int line,
      int charPositionInLine,
      String message,
      RecognitionException exception) {
    String offendingToken = null;
    if (offendingSymbol instanceof Token token) {
      offendingToken = token.getText();
    }
    diagnostics.add(
        new SyntaxDiagnostic(
            DiagnosticSeverity.ERROR,
            message,
            line,
            charPositionInLine,
            offendingToken));
  }

  List<SyntaxDiagnostic> diagnostics() {
    return List.copyOf(diagnostics);
  }

  boolean hasDiagnostics() {
    return !diagnostics.isEmpty();
  }
}
