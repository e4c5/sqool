package io.github.e4c5.sqool.ast;

/** Source span metadata for AST nodes. */
public record SourceSpan(
    int startIndex, int stopIndex, int startLine, int startColumn, int stopLine, int stopColumn) {

  public SourceSpan {
    if (startIndex < 0 || stopIndex < startIndex) {
      throw new IllegalArgumentException("Invalid source indexes.");
    }
    if (startLine < 1 || stopLine < 1) {
      throw new IllegalArgumentException("Line numbers are 1-based.");
    }
    if (startColumn < 0 || stopColumn < 0) {
      throw new IllegalArgumentException("Column numbers are 0-based.");
    }
    if (stopLine < startLine || (stopLine == startLine && stopColumn < startColumn)) {
      throw new IllegalArgumentException("Stop position must not precede start position.");
    }
  }
}
