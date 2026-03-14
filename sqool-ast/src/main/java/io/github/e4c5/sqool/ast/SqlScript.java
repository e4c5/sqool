package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Script root used when parsing multiple statements. */
public record SqlScript(List<Statement> statements, SourceSpan sourceSpan) implements AstNode {

  public SqlScript {
    Objects.requireNonNull(statements, "statements");
    statements = List.copyOf(statements);
  }
}
