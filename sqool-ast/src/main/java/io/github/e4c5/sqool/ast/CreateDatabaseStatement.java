package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Simplified CREATE DATABASE statement AST. */
public record CreateDatabaseStatement(String name, boolean ifNotExists, SourceSpan sourceSpan)
    implements Statement {

  public CreateDatabaseStatement {
    Objects.requireNonNull(name, "name");
  }
}
