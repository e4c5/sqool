package io.github.e4c5.sqool.grammar.postgresql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PostgreSqlGrammarSmokeTest {

  @Test
  void placeholderModuleIsAccessible() {
    assertEquals("postgresql", PostgreSqlGrammarPlaceholder.DIALECT_NAME);
  }
}
