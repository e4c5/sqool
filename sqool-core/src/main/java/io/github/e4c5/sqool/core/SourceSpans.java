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
    String stopText = stop.getText() != null ? stop.getText() : "";
    int lastNewline = stopText.lastIndexOf('\n');
    int endLine;
    int endColumn;
    if (lastNewline >= 0) {
      // Multiline token: the end position is on a different line.
      int newlineCount = 0;
      for (int i = 0; i < stopText.length(); i++) {
        if (stopText.charAt(i) == '\n') {
          newlineCount++;
        }
      }
      endLine = stop.getLine() + newlineCount;
      // endColumn is the 0-based index of the last character on the final line.
      endColumn = Math.max(0, stopText.length() - lastNewline - 2);
    } else {
      endLine = stop.getLine();
      endColumn =
          stopText.isEmpty()
              ? stop.getCharPositionInLine()
              : stop.getCharPositionInLine() + stopText.length() - 1;
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
