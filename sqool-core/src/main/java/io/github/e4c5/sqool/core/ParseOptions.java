package io.github.e4c5.sqool.core;

import java.util.Objects;

/** Parse-time options shared across dialect implementations. */
public record ParseOptions(
    SqlDialect dialect, boolean scriptMode, boolean includeSourceSpans, boolean enableFallback) {

  public ParseOptions {
    Objects.requireNonNull(dialect, "dialect");
  }

  public static ParseOptions defaults(SqlDialect dialect) {
    return new ParseOptions(dialect, false, true, true);
  }

  public ParseOptions withScriptMode(boolean scriptMode) {
    return new ParseOptions(dialect, scriptMode, includeSourceSpans, enableFallback);
  }
}
