package io.github.e4c5.sqool.grammar.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OracleGrammarSmokeTest {

  @Test
  void placeholderModuleIsAccessible() {
    assertEquals("oracle", OracleGrammarPlaceholder.DIALECT_NAME);
  }
}
