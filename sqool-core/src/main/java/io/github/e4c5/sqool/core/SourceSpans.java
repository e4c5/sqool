package io.github.e4c5.sqool.core;

import io.github.e4c5.sqool.ast.SourceSpan;
import org.antlr.v4.runtime.Token;

/** Shared utilities for building {@link SourceSpan} from ANTLR tokens. */
public final class SourceSpans {

  private SourceSpans() {}

  /**
   * Builds a source span from ANTLR start/stop tokens when {@link ParseOptions#includeSourceSpans()}
   * is true; otherwise returns null. Used by dialect AST mappers to avoid duplicating span logic.
   */
  public static SourceSpan fromTokens(Token start, Token stop, ParseOptions options) {
    if (!options.includeSourceSpans() || start == null || stop == null) {
      return null;
    }
    int startColumn = start.getCharPositionInLine();
    int stopColumn = stop.getCharPositionInLine();
    if (stop.getText() != null && !stop.getText().isEmpty()) {
      stopColumn += stop.getText().length() - 1;
    }
    if (start.getLine() == stop.getLine()) {
      stopColumn = Math.max(stopColumn, startColumn);
    }
    return new SourceSpan(
        start.getStartIndex(),
        stop.getStopIndex(),
        start.getLine(),
        startColumn,
        stop.getLine(),
        stopColumn);
  }
}
