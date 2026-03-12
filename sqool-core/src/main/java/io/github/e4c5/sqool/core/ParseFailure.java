package io.github.e4c5.sqool.core;

import java.util.List;
import java.util.Objects;

/** Failed parse result with structured diagnostics. */
public record ParseFailure(SqlDialect dialect, List<SyntaxDiagnostic> diagnostics)
    implements ParseResult {

  public ParseFailure {
    Objects.requireNonNull(dialect, "dialect");
    Objects.requireNonNull(diagnostics, "diagnostics");
    diagnostics = List.copyOf(diagnostics);
  }
}
