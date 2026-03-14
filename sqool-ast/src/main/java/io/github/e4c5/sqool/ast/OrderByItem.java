package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** ORDER BY item in the current AST slice. */
public record OrderByItem(Expression expression, SortDirection direction, SourceSpan sourceSpan)
    implements AstNode {

  public OrderByItem {
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(direction, "direction");
  }
}
