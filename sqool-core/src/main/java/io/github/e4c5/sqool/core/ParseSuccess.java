package io.github.e4c5.sqool.core;

import io.github.e4c5.sqool.ast.AstNode;
import java.util.List;
import java.util.Objects;

/** Successful parse result with a normalized AST root. */
public record ParseSuccess(
    SqlDialect dialect,
    AstNode root,
    List<SyntaxDiagnostic> diagnostics,
    ParseMetrics metrics)
    implements ParseResult {

  public ParseSuccess {
    Objects.requireNonNull(dialect, "dialect");
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(diagnostics, "diagnostics");
    Objects.requireNonNull(metrics, "metrics");
    diagnostics = List.copyOf(diagnostics);
  }

  /** Returns a copy of this result with the given metrics. */
  public ParseSuccess withMetrics(ParseMetrics metrics) {
    return new ParseSuccess(dialect, root, diagnostics, metrics);
  }
}
