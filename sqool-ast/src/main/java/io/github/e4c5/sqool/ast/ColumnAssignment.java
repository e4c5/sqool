package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Assignment used by UPDATE and INSERT ... SET statements. */
public record ColumnAssignment(String column, Expression value, SourceSpan sourceSpan)
    implements AstNode {

  public ColumnAssignment {
    Objects.requireNonNull(column, "column");
    Objects.requireNonNull(value, "value");
  }
}
