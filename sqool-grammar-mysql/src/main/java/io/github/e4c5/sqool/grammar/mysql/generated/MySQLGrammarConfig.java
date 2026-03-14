package io.github.e4c5.sqool.grammar.mysql.generated;

import java.util.*;

/**
 * Shared interface for MySQL grammar components (Parser/Lexer). Provides default implementations
 * for common version and mode checks.
 */
public interface MySQLGrammarConfig {
  /**
   * @return the underlying configuration settings.
   */
  MySQLGrammarSettings getSettings();

  default int getServerVersion() {
    return getSettings().getServerVersion();
  }

  default void setServerVersion(int serverVersion) {
    getSettings().setServerVersion(serverVersion);
  }

  default Set<SqlMode> getSqlModes() {
    return getSettings().getSqlModes();
  }

  default void setSqlModes(Set<SqlMode> sqlModes) {
    getSettings().setSqlModes(sqlModes);
  }

  default boolean isSupportMle() {
    return getSettings().isSupportMle();
  }

  default void setSupportMle(boolean supportMle) {
    getSettings().setSupportMle(supportMle);
  }

  default Set<String> getCharSets() {
    return getSettings().getCharSets();
  }

  default void setCharSets(Set<String> charSets) {
    getSettings().setCharSets(charSets);
  }

  default boolean isSqlModeActive(SqlMode mode) {
    return getSqlModes().contains(mode);
  }

  default boolean isServerVersionGe80004() {
    return getServerVersion() >= 80004;
  }

  default boolean isServerVersionGe80011() {
    return getServerVersion() >= 80011;
  }

  default boolean isServerVersionGe80013() {
    return getServerVersion() >= 80013;
  }

  default boolean isServerVersionGe80014() {
    return getServerVersion() >= 80014;
  }

  default boolean isServerVersionGe80016() {
    return getServerVersion() >= 80016;
  }

  default boolean isServerVersionGe80017() {
    return getServerVersion() >= 80017;
  }

  default boolean isServerVersionGe80018() {
    return getServerVersion() >= 80018;
  }

  default boolean isServerVersionGe80019() {
    return getServerVersion() >= 80019;
  }

  default boolean isServerVersionGe80021() {
    return getServerVersion() >= 80021;
  }

  default boolean isServerVersionGe80022() {
    return getServerVersion() >= 80022;
  }

  default boolean isServerVersionGe80023() {
    return getServerVersion() >= 80023;
  }

  default boolean isServerVersionGe80024() {
    return getServerVersion() >= 80024;
  }

  default boolean isServerVersionGe80025() {
    return getServerVersion() >= 80025;
  }

  default boolean isServerVersionGe80027() {
    return getServerVersion() >= 80027;
  }

  default boolean isServerVersionGe80031() {
    return getServerVersion() >= 80031;
  }

  default boolean isServerVersionGe80032() {
    return getServerVersion() >= 80032;
  }

  default boolean isServerVersionGe80100() {
    return getServerVersion() >= 80100;
  }

  default boolean isServerVersionGe80200() {
    return getServerVersion() >= 80200;
  }

  default boolean isServerVersionLt80011() {
    return getServerVersion() < 80011;
  }

  default boolean isServerVersionLt80012() {
    return getServerVersion() < 80012;
  }

  default boolean isServerVersionLt80014() {
    return getServerVersion() < 80014;
  }

  default boolean isServerVersionLt80016() {
    return getServerVersion() < 80016;
  }

  default boolean isServerVersionLt80017() {
    return getServerVersion() < 80017;
  }

  default boolean isServerVersionLt80021() {
    return getServerVersion() < 80021;
  }

  default boolean isServerVersionLt80022() {
    return getServerVersion() < 80022;
  }

  default boolean isServerVersionLt80023() {
    return getServerVersion() < 80023;
  }

  default boolean isServerVersionLt80024() {
    return getServerVersion() < 80024;
  }

  default boolean isServerVersionLt80025() {
    return getServerVersion() < 80025;
  }

  default boolean isServerVersionLt80031() {
    return getServerVersion() < 80031;
  }
}
