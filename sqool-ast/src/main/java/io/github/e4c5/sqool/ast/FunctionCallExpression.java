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
  }
}
