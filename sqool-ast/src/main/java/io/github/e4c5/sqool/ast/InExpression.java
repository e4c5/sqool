package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** IN predicate expression. */
public record InExpression(
    Expression expression, List<Expression> values, boolean negated, SourceSpan sourceSpan)
    implements Expression {

  public InExpression {
    Objects.requireNonNull(expression, "expression");
    Objects.requireNonNull(values, "values");
    values = List.copyOf(values);
  }
}
