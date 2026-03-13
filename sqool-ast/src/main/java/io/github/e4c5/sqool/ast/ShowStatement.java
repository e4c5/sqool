package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Simplified SHOW statement AST. */
public record ShowStatement(
    ShowStatementKind kind,
    String targetName,
    String schemaName,
    String modifier,
    String filter,
    SourceSpan sourceSpan)
    implements Statement {

  public ShowStatement {
    Objects.requireNonNull(kind, "kind");
  }
}
