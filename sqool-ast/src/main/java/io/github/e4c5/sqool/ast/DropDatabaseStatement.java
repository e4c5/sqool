package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Simplified DROP DATABASE statement AST. */
public record DropDatabaseStatement(String name, boolean ifExists, SourceSpan sourceSpan)
    implements Statement {

  public DropDatabaseStatement {
    Objects.requireNonNull(name, "name");
  }
}
