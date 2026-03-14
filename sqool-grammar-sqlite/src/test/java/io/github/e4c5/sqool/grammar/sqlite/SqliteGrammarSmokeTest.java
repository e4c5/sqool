package io.github.e4c5.sqool.grammar.sqlite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SqliteGrammarSmokeTest {

  @Test
  void placeholderModuleIsAccessible() {
    assertEquals("sqlite", SqliteGrammarPlaceholder.DIALECT_NAME);
  }
}
