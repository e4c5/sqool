package io.github.e4c5.sqool.grammar.mysql.generated;

/*
 * Copyright 2024, Oracle and/or its affiliates
 */

/* eslint-disable no-underscore-dangle */
/* cspell: ignore antlr, longlong, ULONGLONG, MAXDB */

import java.util.*;

/** The base lexer class provides a number of functions needed in actions in the lexer (grammar). */
public class SqlModes {

  /**
   * Converts a mode string into individual mode flags.
   *
   * @param modes The input string to parse.
   */
  public static Set<SqlMode> sqlModeFromString(String modes) {
    Set<SqlMode> result = new HashSet<SqlMode>();

    String[] parts = modes.toUpperCase().split(",");
    for (String mode : parts) {
      switch (mode) {
        case "ANSI":
        case "DB2":
        case "MAXDB":
        case "MSSQL":
        case "ORACLE":
        case "POSTGRESQL":
          result.add(SqlMode.ANSI_QUOTES);
          result.add(SqlMode.PIPES_AS_CONCAT);
          result.add(SqlMode.IGNORE_SPACE);
          break;
        case "ANSI_QUOTES":
          result.add(SqlMode.ANSI_QUOTES);
          break;
        case "PIPES_AS_CONCAT":
          result.add(SqlMode.PIPES_AS_CONCAT);
          break;
        case "NO_BACKSLASH_ESCAPES":
          result.add(SqlMode.NO_BACKSLASH_ESCAPES);
          break;
        case "IGNORE_SPACE":
          result.add(SqlMode.IGNORE_SPACE);
          break;
        case "HIGH_NOT_PRECEDENCE":
        case "MYSQL323":
        case "MYSQL40":
          result.add(SqlMode.HIGH_NOT_PRECEDENCE);
          break;
      }
    }
    return result;
  }
}
