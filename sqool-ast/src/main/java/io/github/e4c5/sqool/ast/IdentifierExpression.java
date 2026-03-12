package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Identifier-like expression supported by the MySQL MVP. */
public record IdentifierExpression(String text, SourceSpan sourceSpan) implements Expression {

  public IdentifierExpression {
    Objects.requireNonNull(text, "text");
  }
}
