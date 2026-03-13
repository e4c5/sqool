package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Raw MySQL statement fallback for valid syntax not yet normalized into a richer AST shape. */
public record MySqlRawStatement(MySqlStatementKind kind, String sqlText, SourceSpan sourceSpan)
    implements Statement {

  public MySqlRawStatement {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(sqlText, "sqlText");
  }
}
