package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Simplified DROP TABLE statement AST. */
public record DropTableStatement(
    List<String> tableNames, boolean ifExists, boolean temporary, SourceSpan sourceSpan)
    implements Statement {

  public DropTableStatement {
    Objects.requireNonNull(tableNames, "tableNames");
    tableNames = List.copyOf(tableNames);
  }
}
