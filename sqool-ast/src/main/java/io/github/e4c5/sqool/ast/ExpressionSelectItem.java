package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Projection item backed by an expression. */
public record ExpressionSelectItem(Expression expression, String alias, SourceSpan sourceSpan)
    implements SelectItem {

  public ExpressionSelectItem {
    Objects.requireNonNull(expression, "expression");
  }
}
