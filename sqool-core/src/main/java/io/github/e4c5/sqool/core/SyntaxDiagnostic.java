package io.github.e4c5.sqool.core;

import java.util.Objects;

/** Structured syntax or capability diagnostic. */
public record SyntaxDiagnostic(
    DiagnosticSeverity severity, String message, int line, int column, String offendingToken) {

  public SyntaxDiagnostic {
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(message, "message");
    if (message.isBlank()) {
      throw new IllegalArgumentException("message must not be blank.");
    }
    if (line < 1) {
      throw new IllegalArgumentException("Line numbers are 1-based.");
    }
    if (column < 0) {
      throw new IllegalArgumentException("Column numbers are 0-based.");
    }
  }
}
