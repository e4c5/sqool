package io.github.e4c5.sqool.core;

/**
 * Carries the result of an AST-mapping attempt. {@code supported} is false when the input shape
 * falls outside the normalized subset and the caller should fall back to a raw statement.
 */
public record MappingResult<T>(boolean supported, T value) {}
