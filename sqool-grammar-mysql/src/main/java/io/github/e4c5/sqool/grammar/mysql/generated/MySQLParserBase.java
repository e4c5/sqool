package io.github.e4c5.sqool.grammar.mysql.generated;

/*
 * Copyright 2024, Oracle and/or its affiliates
 */

import java.io.*;
import java.util.*;
import org.antlr.v4.runtime.*;

public abstract class MySQLParserBase extends Parser implements MySQLGrammarConfig {
  private final MySQLGrammarSettings settings = new MySQLGrammarSettings();

  @Override
  public MySQLGrammarSettings getSettings() {
    return settings;
  }

  protected MySQLParserBase(TokenStream input) {
    super(input);
  }

  public boolean isPureIdentifier() {
    return this.isSqlModeActive(SqlMode.ANSI_QUOTES);
  }

  public boolean isTextStringLiteral() {
    return !this.isSqlModeActive(SqlMode.ANSI_QUOTES);
  }

  public boolean isStoredRoutineBody() {
    return getServerVersion() >= 80032 && isSupportMle();
  }

  public boolean isSelectStatementWithInto() {
    return getServerVersion() >= 80024 && getServerVersion() < 80031;
  }
}
