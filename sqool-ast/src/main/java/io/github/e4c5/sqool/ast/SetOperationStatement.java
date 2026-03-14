package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Binary set operation statement such as UNION or UNION ALL. */
public record SetOperationStatement(
    Statement left,
    SetOperator operator,
    Statement right,
    List<OrderByItem> orderBy,
    LimitClause limit,
    SourceSpan sourceSpan)
    implements Statement {

  public SetOperationStatement {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(operator, "operator");
    Objects.requireNonNull(right, "right");
    Objects.requireNonNull(orderBy, "orderBy");
    orderBy = List.copyOf(orderBy);
  }
}
