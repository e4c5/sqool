package io.github.e4c5.sqool.ast;

import java.util.List;
import java.util.Objects;

/** Simplified INSERT statement AST. */
public record InsertStatement(
    String tableName,
    List<String> columns,
    List<List<Expression>> rows,
    List<ColumnAssignment> assignments,
    Statement sourceQuery,
    List<ColumnAssignment> onDuplicateKeyAssignments,
    boolean ignore,
    SourceSpan sourceSpan)
    implements Statement {

  public InsertStatement {
    Objects.requireNonNull(tableName, "tableName");
    Objects.requireNonNull(columns, "columns");
    Objects.requireNonNull(rows, "rows");
    Objects.requireNonNull(assignments, "assignments");
    Objects.requireNonNull(onDuplicateKeyAssignments, "onDuplicateKeyAssignments");
    columns = List.copyOf(columns);
    rows = rows.stream().map(List::copyOf).toList();
    assignments = List.copyOf(assignments);
    onDuplicateKeyAssignments = List.copyOf(onDuplicateKeyAssignments);
  }
}
