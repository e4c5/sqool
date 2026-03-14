package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Raw SQLite statement fallback for valid syntax not yet normalized into a richer AST shape. */
public record SqliteRawStatement(SqliteStatementKind kind, String sqlText, SourceSpan sourceSpan)
    implements Statement {

  public SqliteRawStatement {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(sqlText, "sqlText");
  }
}
