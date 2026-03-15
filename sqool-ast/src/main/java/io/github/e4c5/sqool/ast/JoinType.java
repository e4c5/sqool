package io.github.e4c5.sqool.ast;

/** Supported join types in the current AST slice. */
public enum JoinType {
  CROSS,
  FULL,
  INNER,
  LEFT,
  RIGHT,
  STRAIGHT;

  /**
   * Resolves the join type from the OUTER keyword flags present on a {@code joinKind} parse-tree
   * node. Call this when the context is non-null; pass {@code null ctx} as {@code JoinType.INNER}
   * directly.
   *
   * @param hasLeft whether the LEFT keyword was present
   * @param hasRight whether the RIGHT keyword was present
   * @param hasFull whether the FULL keyword was present
   */
  public static JoinType fromKind(boolean hasLeft, boolean hasRight, boolean hasFull) {
    if (hasLeft) return LEFT;
    if (hasRight) return RIGHT;
    if (hasFull) return FULL;
    return INNER;
  }
}
