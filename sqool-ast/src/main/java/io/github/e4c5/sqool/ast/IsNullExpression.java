package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** IS [NOT] NULL predicate expression. */
public record IsNullExpression(Expression expression, boolean negated, SourceSpan sourceSpan)
    implements Expression {

  public IsNullExpression {
    Objects.requireNonNull(expression, "expression");
  }
}
