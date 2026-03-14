package io.github.e4c5.sqool.ast;

/** LIMIT clause represented as row count plus optional offset. */
public record LimitClause(long rowCount, Long offset, SourceSpan sourceSpan) implements AstNode {

  public LimitClause {
    if (rowCount < 0) {
      throw new IllegalArgumentException("rowCount must be non-negative: " + rowCount);
    }
    if (offset != null && offset < 0) {
      throw new IllegalArgumentException("offset must be non-negative: " + offset);
    }
  }
}
