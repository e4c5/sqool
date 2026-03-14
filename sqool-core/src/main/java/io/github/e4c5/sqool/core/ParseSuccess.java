package io.github.e4c5.sqool.core;

import io.github.e4c5.sqool.ast.AstNode;
import java.util.List;
import java.util.Objects;

/** Successful parse result with a normalized AST root. */
public record ParseSuccess(SqlDialect dialect, AstNode root, List<SyntaxDiagnostic> diagnostics)
    implements ParseResult {

  public ParseSuccess {
    Objects.requireNonNull(dialect, "dialect");
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(diagnostics, "diagnostics");
    diagnostics = List.copyOf(diagnostics);
  }
}
