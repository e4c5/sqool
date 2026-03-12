package io.github.e4c5.sqool.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SqoolCoreTest {

  @Test
  void exposesStableProjectName() {
    assertEquals("sqool", SqoolCore.PROJECT_NAME);
  }
}
