package io.github.e4c5.sqool.ast;

import java.util.Objects;

/** Joined table reference with an optional ON condition. */
public record JoinTableReference(
    TableReference left,
    JoinType joinType,
    TableReference right,
    Expression condition,
    SourceSpan sourceSpan)
    implements TableReference {

  public JoinTableReference {
    Objects.requireNonNull(left, "left");
    Objects.requireNonNull(joinType, "joinType");
    Objects.requireNonNull(right, "right");
  }
}
