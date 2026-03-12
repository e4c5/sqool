package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Minimal normalized SELECT statement for the MySQL MVP. */
public record SelectStatement(
    List<SelectItem> selectItems,
    TableReference from,
    Expression where,
    List<OrderByItem> orderBy,
    LimitClause limit,
    SourceSpan sourceSpan)
    implements Statement {

  public SelectStatement {
    Objects.requireNonNull(selectItems, "selectItems");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(orderBy, "orderBy");
    selectItems = List.copyOf(selectItems);
    orderBy = List.copyOf(orderBy);
  }
}
