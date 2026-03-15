package io.github.e4c5.sqool.ast;

/** Supported join types in the current AST slice. */
public enum JoinType {
  CROSS,
  FULL,
  INNER,
  LEFT,
  RIGHT,
  STRAIGHT
}
