package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** LIKE predicate expression. */
public record LikeExpression(
    Expression expression,
    Expression pattern,
    Expression escape,
    boolean negated,
    SourceSpan sourceSpan)
    implements Expression {

  public LikeExpression {
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(pattern, "pattern");
  }
}
