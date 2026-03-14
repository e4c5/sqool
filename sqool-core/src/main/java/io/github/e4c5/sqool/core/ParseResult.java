package io.github.e4c5.sqool.core;

import java.util.List;

/** Result of a parser invocation. */
public sealed interface ParseResult permits ParseSuccess, ParseFailure {

  SqlDialect dialect();

  List<SyntaxDiagnostic> diagnostics();

  default boolean isSuccess() {
    return this instanceof ParseSuccess;
  }
}
