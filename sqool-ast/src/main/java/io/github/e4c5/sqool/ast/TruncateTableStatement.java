package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Simplified TRUNCATE TABLE statement AST. */
public record TruncateTableStatement(String tableName, SourceSpan sourceSpan) implements Statement {

  public TruncateTableStatement {
    Objects.requireNonNull(tableName, "tableName");
  }
}
