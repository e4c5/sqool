package io.github.e4c5.sqool.ast;

/** Identifies the kind of a raw Oracle statement that was not fully mapped into the AST. */
public enum OracleStatementKind {
  SELECT,
  INSERT,
  UPDATE,
  DELETE,
  CREATE_TABLE,
  DROP_TABLE,
  TRUNCATE,
  COMMIT,
  ROLLBACK,
  SAVEPOINT,
  OTHER
}
