package io.github.e4c5.sqool.core;

import java.util.List;

/**
 * Result of a single ANTLR parse attempt: the rule context and any syntax diagnostics. Shared by
 * all dialects to avoid duplicating the attempt/failure pattern.
 *
 * @param <C> the ANTLR rule context type (e.g. SimpleStatementContext, ParseContext)
 */
public record ParseAttempt<C>(C context, List<SyntaxDiagnostic> diagnostics) {

  public ParseAttempt {
    diagnostics = diagnostics != null ? List.copyOf(diagnostics) : List.of();
  }

  public static <C> ParseAttempt<C> failure(List<SyntaxDiagnostic> diagnostics) {
    return new ParseAttempt<>(null, diagnostics);
  }
}
