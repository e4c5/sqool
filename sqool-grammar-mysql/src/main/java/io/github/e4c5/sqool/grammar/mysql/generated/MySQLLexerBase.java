package io.github.e4c5.sqool.grammar.mysql.generated;

/*
 * Copyright 2024, Oracle and/or its affiliates
 */

/* eslint-disable no-underscore-dangle */
/* cspell: ignore antlr, longlong, ULONGLONG, MAXDB */

import java.util.*;
import org.antlr.v4.runtime.*;

/** The base lexer class provides a number of functions needed in actions in the lexer (grammar). */
public abstract class MySQLLexerBase extends Lexer implements MySQLGrammarConfig {
  private final MySQLGrammarSettings settings = new MySQLGrammarSettings();

  @Override
  public MySQLGrammarSettings getSettings() {
    return settings;
  }

  protected MySQLLexerBase(CharStream input) {
    super(input);
  }

  protected boolean inVersionComment = false;

  private Queue<Token> pendingTokens = new LinkedList<>();

  static String longString = "2147483647";
  static int longLength = 10;
  static String signedLongString = "-2147483648";
  static String longLongString = "9223372036854775807";
  static int longLongLength = 19;
  static String signedLongLongString = "-9223372036854775808";
  static int signedLongLongLength = 19;
  static String unsignedLongLongString = "18446744073709551615";
  static int unsignedLongLongLength = 20;

  private boolean justEmittedDot = false;

  @Override
  public void reset() {
    this.inVersionComment = false;
    super.reset();
  }

  @Override
  public Token nextToken() {
    Token pending = pendingTokens.poll();
    if (pending != null) {
      return pending;
    }

    Token next = super.nextToken();

    pending = pendingTokens.poll();
    if (pending != null) {
      pendingTokens.add(next);
      return pending;
    }

    return next;
  }

  protected boolean checkMySQLVersion(String text) {
    if (text.length() < 8) { // Minimum is: /*!12345
      return false;
    }

    int version = Integer.parseInt(text.substring(3));
    if (version <= getServerVersion()) {
      this.inVersionComment = true;
      return true;
    }

    return false;
  }

  protected int determineFunction(int proposed) {
    char input = (char) this._input.LA(1);
    if (this.isSqlModeActive(SqlMode.IGNORE_SPACE)) {
      while (input == ' ' || input == '\t' || input == '\r' || input == '\n') {
        this.getInterpreter().consume(this._input);
        this._channel = HIDDEN;
        this._type = MySQLLexer.WHITESPACE;
        input = (char) this._input.LA(1);
      }
    }
    return input == '(' ? proposed : MySQLLexer.IDENTIFIER;
  }

  protected int determineNumericType(String text) {
    int length = text.length() - 1;
    if (length < MySQLLexerBase.longLength) {
      return MySQLLexer.INT_NUMBER;
    }

    int index = 0;
    boolean negative = false;
    if (text.charAt(index) == '+') {
      ++index;
      --length;
    } else if (text.charAt(index) == '-') {
      ++index;
      --length;
      negative = true;
    }

    while (text.charAt(index) == '0' && length > 0) {
      ++index;
      --length;
    }

    if (length < MySQLLexerBase.longLength) {
      return MySQLLexer.INT_NUMBER;
    }

    return determineTypeByRange(text, index, length, negative);
  }

  private int determineTypeByRange(String text, int index, int length, boolean negative) {
    Integer early = negative ? negativeEarlyReturn(length) : positiveEarlyReturn(length);
    if (early != null) {
      return early;
    }
    CompareLimit limit = negative ? negativeCompareLimit(length) : positiveCompareLimit(length);
    return compareWithLimit(text, index, limit.cmp, limit.smaller, limit.bigger);
  }

  /** Returns token type for early exit, or null if comparison is needed. */
  private Integer negativeEarlyReturn(int length) {
    if (length < MySQLLexerBase.signedLongLongLength) {
      return length == MySQLLexerBase.longLength ? null : MySQLLexer.LONG_NUMBER;
    }
    return length > MySQLLexerBase.signedLongLongLength ? MySQLLexer.DECIMAL_NUMBER : null;
  }

  private CompareLimit negativeCompareLimit(int length) {
    if (length == MySQLLexerBase.longLength) {
      return new CompareLimit(
          MySQLLexerBase.signedLongString.substring(1),
          MySQLLexer.INT_NUMBER,
          MySQLLexer.LONG_NUMBER);
    }
    return new CompareLimit(
        MySQLLexerBase.signedLongLongString.substring(1),
        MySQLLexer.LONG_NUMBER,
        MySQLLexer.DECIMAL_NUMBER);
  }

  /** Returns token type for early exit, or null if comparison is needed. */
  private Integer positiveEarlyReturn(int length) {
    if (length < MySQLLexerBase.longLongLength) {
      return length == MySQLLexerBase.longLength ? null : MySQLLexer.LONG_NUMBER;
    }
    if (length > MySQLLexerBase.longLongLength) {
      return length > MySQLLexerBase.unsignedLongLongLength
          ? MySQLLexer.DECIMAL_NUMBER
          : null;
    }
    return null;
  }

  private CompareLimit positiveCompareLimit(int length) {
    if (length == MySQLLexerBase.longLength) {
      return new CompareLimit(
          MySQLLexerBase.longString, MySQLLexer.INT_NUMBER, MySQLLexer.LONG_NUMBER);
    }
    if (length > MySQLLexerBase.longLongLength) {
      return new CompareLimit(
          MySQLLexerBase.unsignedLongLongString,
          MySQLLexer.ULONGLONG_NUMBER,
          MySQLLexer.DECIMAL_NUMBER);
    }
    return new CompareLimit(
        MySQLLexerBase.longLongString, MySQLLexer.LONG_NUMBER, MySQLLexer.ULONGLONG_NUMBER);
  }

  private record CompareLimit(String cmp, int smaller, int bigger) {}

  private int compareWithLimit(String text, int index, String cmp, int smaller, int bigger) {
    int otherIndex = 0;
    while (index < text.length() && cmp.charAt(otherIndex++) == text.charAt(index++)) {
      // Find the first differing character
    }

    return text.charAt(index - 1) <= cmp.charAt(otherIndex - 1) ? smaller : bigger;
  }

  protected int checkCharset(String text) {
    return getCharSets().contains(text) ? MySQLLexer.UNDERSCORE_CHARSET : MySQLLexer.IDENTIFIER;
  }

  protected void emitDot() {
    var len = this.getText().length();
    pendingTokens.add(
        this._factory.create(
            this._tokenFactorySourcePair,
            MySQLLexer.DOT_SYMBOL,
            ".",
            this._channel,
            this._tokenStartCharIndex,
            this._tokenStartCharIndex,
            this.getLine(),
            this.getCharPositionInLine() - len));
    ++this._tokenStartCharPositionInLine;
    this.justEmittedDot = true;
  }

  @Override
  public Token emit() {
    var t = super.emit();
    if (this.justEmittedDot) {
      var p = (CommonToken) t;
      p.setText(p.getText().substring(1));
      p.setStartIndex(p.getStartIndex() + 1);
      this.justEmittedDot = false;
    }
    return t;
  }

  public boolean isMasterCompressionAlgorithm() {
    return getServerVersion() >= 80018 && isServerVersionLt80024();
  }

  public void doLogicalOr() {
    this._type =
        isSqlModeActive(SqlMode.PIPES_AS_CONCAT)
            ? MySQLLexer.CONCAT_PIPES_SYMBOL
            : MySQLLexer.LOGICAL_OR_OPERATOR;
  }

  public void doIntNumber() {
    this._type = determineNumericType(this.getText());
  }

  public void doAdddate() {
    this._type = determineFunction(MySQLLexer.ADDDATE_SYMBOL);
  }

  public void doBitAnd() {
    this._type = determineFunction(MySQLLexer.BIT_AND_SYMBOL);
  }

  public void doBitOr() {
    this._type = determineFunction(MySQLLexer.BIT_OR_SYMBOL);
  }

  public void doBitXor() {
    this._type = determineFunction(MySQLLexer.BIT_XOR_SYMBOL);
  }

  public void doCast() {
    this._type = determineFunction(MySQLLexer.CAST_SYMBOL);
  }

  public void doCount() {
    this._type = determineFunction(MySQLLexer.COUNT_SYMBOL);
  }

  public void doCurdate() {
    this._type = determineFunction(MySQLLexer.CURDATE_SYMBOL);
  }

  public void doCurrentDate() {
    this._type = determineFunction(MySQLLexer.CURDATE_SYMBOL);
  }

  public void doCurrentTime() {
    this._type = determineFunction(MySQLLexer.CURTIME_SYMBOL);
  }

  public void doCurtime() {
    this._type = determineFunction(MySQLLexer.CURTIME_SYMBOL);
  }

  public void doDateAdd() {
    this._type = determineFunction(MySQLLexer.DATE_ADD_SYMBOL);
  }

  public void doDateSub() {
    this._type = determineFunction(MySQLLexer.DATE_SUB_SYMBOL);
  }

  public void doExtract() {
    this._type = determineFunction(MySQLLexer.EXTRACT_SYMBOL);
  }

  public void doGroupConcat() {
    this._type = determineFunction(MySQLLexer.GROUP_CONCAT_SYMBOL);
  }

  public void doMax() {
    this._type = determineFunction(MySQLLexer.MAX_SYMBOL);
  }

  public void doMid() {
    this._type = determineFunction(MySQLLexer.SUBSTRING_SYMBOL);
  }

  public void doMin() {
    this._type = determineFunction(MySQLLexer.MIN_SYMBOL);
  }

  public void doNot() {
    this._type =
        isSqlModeActive(SqlMode.HIGH_NOT_PRECEDENCE)
            ? MySQLLexer.NOT2_SYMBOL
            : MySQLLexer.NOT_SYMBOL;
  }

  public void doNow() {
    this._type = determineFunction(MySQLLexer.NOW_SYMBOL);
  }

  public void doPosition() {
    this._type = determineFunction(MySQLLexer.POSITION_SYMBOL);
  }

  public void doSessionUser() {
    this._type = determineFunction(MySQLLexer.USER_SYMBOL);
  }

  public void doStddevSamp() {
    this._type = determineFunction(MySQLLexer.STDDEV_SAMP_SYMBOL);
  }

  public void doStddev() {
    this._type = determineFunction(MySQLLexer.STD_SYMBOL);
  }

  public void doStddevPop() {
    this._type = determineFunction(MySQLLexer.STD_SYMBOL);
  }

  public void doStd() {
    this._type = determineFunction(MySQLLexer.STD_SYMBOL);
  }

  public void doSubdate() {
    this._type = determineFunction(MySQLLexer.SUBDATE_SYMBOL);
  }

  public void doSubstr() {
    this._type = determineFunction(MySQLLexer.SUBSTRING_SYMBOL);
  }

  public void doSubstring() {
    this._type = determineFunction(MySQLLexer.SUBSTRING_SYMBOL);
  }

  public void doSum() {
    this._type = determineFunction(MySQLLexer.SUM_SYMBOL);
  }

  public void doSysdate() {
    this._type = determineFunction(MySQLLexer.SYSDATE_SYMBOL);
  }

  public void doSystemUser() {
    this._type = determineFunction(MySQLLexer.USER_SYMBOL);
  }

  public void doTrim() {
    this._type = determineFunction(MySQLLexer.TRIM_SYMBOL);
  }

  public void doVariance() {
    this._type = determineFunction(MySQLLexer.VARIANCE_SYMBOL);
  }

  public void doVarPop() {
    this._type = determineFunction(MySQLLexer.VARIANCE_SYMBOL);
  }

  public void doVarSamp() {
    this._type = determineFunction(MySQLLexer.VAR_SAMP_SYMBOL);
  }

  public void doUnderscoreCharset() {
    this._type = checkCharset(this.getText());
  }

  public boolean doDollarQuotedStringText() {
    return getServerVersion() >= 80034 && isSupportMle();
  }

  public boolean isVersionComment() {
    return checkMySQLVersion(this.getText());
  }

  public boolean isBackTickQuotedId() {
    return !this.isSqlModeActive(SqlMode.NO_BACKSLASH_ESCAPES);
  }

  public boolean isDoubleQuotedText() {
    return !this.isSqlModeActive(SqlMode.NO_BACKSLASH_ESCAPES);
  }

  public boolean isSingleQuotedText() {
    return !this.isSqlModeActive(SqlMode.NO_BACKSLASH_ESCAPES);
  }

  public void startInVersionComment() {
    inVersionComment = true;
  }

  public void endInVersionComment() {
    inVersionComment = false;
  }

  public boolean isInVersionComment() {
    return inVersionComment;
  }
}
