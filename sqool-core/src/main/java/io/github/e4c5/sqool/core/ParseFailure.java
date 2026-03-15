package io.github.e4c5.sqool.core;

import java.util.List;
import java.util.Objects;

/** Failed parse result with structured diagnostics. */
public record ParseFailure(
    SqlDialect dialect, List<SyntaxDiagnostic> diagnostics, ParseMetrics metrics)
    implements ParseResult {

  public ParseFailure {
    Objects.requireNonNull(dialect, "dialect");
    Objects.requireNonNull(diagnostics, "diagnostics");
    Objects.requireNonNull(metrics, "metrics");
    diagnostics = List.copyOf(diagnostics);
  }

  /** Returns a copy of this result with the given metrics. */
  public ParseFailure withMetrics(ParseMetrics metrics) {
    return new ParseFailure(dialect, diagnostics, metrics);
  }
}
