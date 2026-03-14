package io.github.e4c5.sqool.grammar.mysql.generated;

import java.util.*;

/** Common configuration for MySQL grammar parser and lexer. */
public final class MySQLGrammarSettings {
  private int serverVersion = 80200;
  private Set<SqlMode> sqlModes = SqlModes.sqlModeFromString("ANSI_QUOTES");
  private boolean supportMle = true;
  private Set<String> charSets = new HashSet<>();

  public int getServerVersion() {
    return serverVersion;
  }

  public void setServerVersion(int serverVersion) {
    this.serverVersion = serverVersion;
  }

  public Set<SqlMode> getSqlModes() {
    return sqlModes;
  }

  public void setSqlModes(Set<SqlMode> sqlModes) {
    this.sqlModes = sqlModes == null ? new HashSet<>() : new HashSet<>(sqlModes);
  }

  public boolean isSupportMle() {
    return supportMle;
  }

  public void setSupportMle(boolean supportMle) {
    this.supportMle = supportMle;
  }

  public Set<String> getCharSets() {
    return charSets;
  }

  public void setCharSets(Set<String> charSets) {
    this.charSets = charSets == null ? new HashSet<>() : new HashSet<>(charSets);
  }
}
