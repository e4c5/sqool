package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Simplified DELETE statement AST. */
public record DeleteStatement(
    TableReference target,
    Expression where,
    List<OrderByItem> orderBy,
    LimitClause limit,
    SourceSpan sourceSpan)
    implements Statement {

  public DeleteStatement {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(orderBy, "orderBy");
    orderBy = List.copyOf(orderBy);
  }
}
