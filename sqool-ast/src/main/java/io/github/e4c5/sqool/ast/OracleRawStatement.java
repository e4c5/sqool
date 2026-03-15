package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Raw Oracle statement fallback for valid syntax not yet normalized into a richer AST shape. */
public record OracleRawStatement(OracleStatementKind kind, String sqlText, SourceSpan sourceSpan)
    implements Statement {

  public OracleRawStatement {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(sqlText, "sqlText");
  }
}
