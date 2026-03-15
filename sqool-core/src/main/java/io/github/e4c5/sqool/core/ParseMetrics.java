package io.github.e4c5.sqool.core;

import java.util.Objects;

/**
 * Metrics captured during a parse attempt, for benchmarking and debugging.
 *
 * <p>Includes the prediction mode used (SLL fast path vs LL fallback) and elapsed time. No ANTLR
 * types are exposed; the prediction mode is represented as an enum to keep the public API
 * dialect-agnostic.
 */
public record ParseMetrics(PredictionMode predictionMode, long parseTimeNanos) {

  public ParseMetrics {
    Objects.requireNonNull(predictionMode, "predictionMode");
    if (parseTimeNanos < 0) {
      throw new IllegalArgumentException("parseTimeNanos must be non-negative");
    }
  }

  /**
   * Creates metrics for a successful parse.
   *
   * @param predictionMode SLL or LL
   * @param parseTimeNanos elapsed time of the parse in nanoseconds
   * @return new ParseMetrics instance
   */
  public static ParseMetrics of(PredictionMode predictionMode, long parseTimeNanos) {
    return new ParseMetrics(predictionMode, parseTimeNanos);
  }

  /** Sentinel used when metrics were not recorded (e.g. from AST mapper). */
  private static final ParseMetrics UNKNOWN = new ParseMetrics(PredictionMode.UNKNOWN, 0L);

  /** Returns metrics for when prediction mode and timing were not recorded. */
  public static ParseMetrics unknown() {
    return UNKNOWN;
  }

  /** Prediction strategy used by the parser (SLL = fast, LL = fallback). */
  public enum PredictionMode {
    SLL,
    LL,
    /** Used when the parser did not record which mode was used. */
    UNKNOWN
  }
}
