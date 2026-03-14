package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Unary expression node. */
public record UnaryExpression(UnaryOperator operator, Expression expression, SourceSpan sourceSpan)
    implements Expression {

  public UnaryExpression {
    Objects.requireNonNull(operator, "operator");
    Objects.requireNonNull(expression, "expression");
  }
}
