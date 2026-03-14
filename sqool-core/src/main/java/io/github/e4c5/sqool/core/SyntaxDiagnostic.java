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
    line = Math.max(1, line);
    column = Math.max(0, column);
  }
}
