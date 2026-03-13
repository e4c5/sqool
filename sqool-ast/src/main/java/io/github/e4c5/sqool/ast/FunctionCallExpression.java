package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Function or aggregate call expression. */
public record FunctionCallExpression(
    String name,
    List<Expression> arguments,
    boolean distinct,
    boolean starArgument,
    SourceSpan sourceSpan)
    implements Expression {

  public FunctionCallExpression {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(arguments, "arguments");
    arguments = List.copyOf(arguments);
    if (starArgument && !arguments.isEmpty()) {
      throw new IllegalArgumentException("starArgument requires empty arguments.");
    }
    if (starArgument && distinct) {
      throw new IllegalArgumentException("starArgument cannot be combined with distinct.");
    }
  }
}
