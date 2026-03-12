package io.github.e4c5.sqool.core;

/** Dialect parser contract. */
public interface SqlParser {

  ParseResult parse(String sql, ParseOptions options);
}
