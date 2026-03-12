package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Minimal normalized SELECT statement for the MySQL MVP. */
public record SelectStatement(
    boolean distinct,
    List<SelectItem> selectItems,
    TableReference from,
    Expression where,
    List<Expression> groupBy,
    Expression having,
    List<OrderByItem> orderBy,
    LimitClause limit,
    SourceSpan sourceSpan)
    implements Statement {

  public SelectStatement {
    Objects.requireNonNull(selectItems, "selectItems");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(groupBy, "groupBy");
    Objects.requireNonNull(orderBy, "orderBy");
    selectItems = List.copyOf(selectItems);
    groupBy = List.copyOf(groupBy);
    orderBy = List.copyOf(orderBy);
  }
}
