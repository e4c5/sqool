package io.github.e4c5.sqool.ast;

import java.util.Objects;

/**
 * Raw PostgreSQL statement fallback for valid syntax not yet normalized into a richer AST shape.
 */
public record PostgresqlRawStatement(
    PostgresqlStatementKind kind, String sqlText, SourceSpan sourceSpan) implements Statement {

  public PostgresqlRawStatement {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(sqlText, "sqlText");
  }
}
