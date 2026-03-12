package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** BETWEEN predicate expression. */
public record BetweenExpression(
    Expression expression,
    Expression lowerBound,
    Expression upperBound,
    boolean negated,
    SourceSpan sourceSpan)
    implements Expression {

  public BetweenExpression {
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(lowerBound, "lowerBound");
    Objects.requireNonNull(upperBound, "upperBound");
  }
}
