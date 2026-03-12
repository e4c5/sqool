package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Minimal normalized SELECT statement for the MySQL MVP. */
public record SelectStatement(
    List<SelectItem> selectItems, TableReference from, SourceSpan sourceSpan) implements Statement {

  public SelectStatement {
    Objects.requireNonNull(selectItems, "selectItems");
    Objects.requireNonNull(from, "from");
    selectItems = List.copyOf(selectItems);
  }
}
