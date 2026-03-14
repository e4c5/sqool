package io.github.e4c5.sqool.core;

import io.github.e4c5.sqool.ast.SourceSpan;
import org.antlr.v4.runtime.Token;

/** Shared utilities for building {@link SourceSpan} from ANTLR tokens. */
public final class SourceSpans {

  private SourceSpans() {}

  /**
   * Builds a source span from ANTLR start/stop tokens when {@link
   * ParseOptions#includeSourceSpans()} is true; otherwise returns null. Used by dialect AST mappers
   * to avoid duplicating span logic.
   */
  public static SourceSpan fromTokens(Token start, Token stop, ParseOptions options) {
    if (!options.includeSourceSpans() || start == null || stop == null) {
      return null;
    }
    int startColumn = start.getCharPositionInLine();
    int endLine = stop.getLine();
    int endColumn;
    String stopText = stop.getText();
    if (stopText != null && !stopText.isEmpty()) {
      int lastNewline = stopText.lastIndexOf('\n');
      if (lastNewline >= 0) {
        // Token spans multiple lines: end line advances and end column is text after last newline.
        long newlineCount = stopText.chars().filter(c -> c == '\n').count();
        endLine = stop.getLine() + (int) newlineCount;
        endColumn = stopText.length() - lastNewline - 2;
        if (endColumn < 0) {
          endColumn = 0;
        }
      } else {
        endColumn = stop.getCharPositionInLine() + stopText.length() - 1;
      }
    } else {
      endColumn = stop.getCharPositionInLine();
    }
    if (start.getLine() == endLine) {
      endColumn = Math.max(endColumn, startColumn);
    }
    return new SourceSpan(
        start.getStartIndex(),
        stop.getStopIndex(),
        start.getLine(),
        startColumn,
        endLine,
        endColumn);
  }
}
