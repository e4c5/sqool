package io.github.e4c5.sqool.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AstNodeTest {

  @Test
  void markerInterfaceHasStableCanonicalName() {
    assertEquals("io.github.e4c5.sqool.ast.AstNode", AstNode.class.getCanonicalName());
  }
}
