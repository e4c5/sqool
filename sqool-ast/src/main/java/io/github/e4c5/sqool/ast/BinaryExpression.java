package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Binary expression node. */
public record BinaryExpression(
    Expression left, BinaryOperator operator, Expression right, SourceSpan sourceSpan)
    implements Expression {

  public BinaryExpression {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(operator, "operator");
    Objects.requireNonNull(right, "right");
  }
}
