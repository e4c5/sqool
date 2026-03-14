package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Simplified UPDATE statement AST. */
public record UpdateStatement(
    TableReference target,
    List<ColumnAssignment> assignments,
    Expression where,
    List<OrderByItem> orderBy,
    LimitClause limit,
    boolean ignore,
    SourceSpan sourceSpan)
    implements Statement {

  public UpdateStatement {
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(assignments, "assignments");
    Objects.requireNonNull(orderBy, "orderBy");
    assignments = List.copyOf(assignments);
    orderBy = List.copyOf(orderBy);
  }
}
