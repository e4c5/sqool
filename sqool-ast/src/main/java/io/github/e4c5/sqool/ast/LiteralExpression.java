package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Literal expression represented by its source text. */
public record LiteralExpression(String text, SourceSpan sourceSpan) implements Expression {

  public LiteralExpression {
    Objects.requireNonNull(text, "text");
  }
}
