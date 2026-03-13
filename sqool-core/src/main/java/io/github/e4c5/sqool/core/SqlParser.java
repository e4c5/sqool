package io.github.e4c5.sqool.core;

/**
 * Dialect parser contract.
 *
 * <p>Implementations of {@link SqlParser} should return a {@link ParseResult} for all user input
 * and avoid throwing for malformed SQL. Invalid SQL (including {@code null} or blank input) should
 * be reported as {@link ParseFailure} with clear, actionable diagnostics and position metadata when
 * available.
 *
 * <p>The {@code options} argument should be treated defensively: a {@code null} value is equivalent
 * to a parser-specific default {@link ParseOptions} instance for the implementation's dialect.
 */
public interface SqlParser {

  /**
   * Parses the supplied SQL text according to the provided {@link ParseOptions}.
   *
   * @param sql SQL input to parse
   * @param options parse options (nullable; implementations should substitute sensible defaults)
   * @return {@link ParseResult} containing either a successful AST root or a {@link ParseFailure}
   */
  ParseResult parse(String sql, ParseOptions options);
}
