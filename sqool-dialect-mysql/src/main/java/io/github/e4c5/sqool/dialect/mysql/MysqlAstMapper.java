package io.github.e4c5.sqool.dialect.mysql;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.BinaryExpression;
import io.github.e4c5.sqool.ast.BinaryOperator;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.IdentifierExpression;
import io.github.e4c5.sqool.ast.JoinTableReference;
import io.github.e4c5.sqool.ast.JoinType;
import io.github.e4c5.sqool.ast.LimitClause;
import io.github.e4c5.sqool.ast.LiteralExpression;
import io.github.e4c5.sqool.ast.NamedTableReference;
import io.github.e4c5.sqool.ast.OrderByItem;
import io.github.e4c5.sqool.ast.SelectItem;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.SortDirection;
import io.github.e4c5.sqool.ast.SourceSpan;
import io.github.e4c5.sqool.ast.TableReference;
import io.github.e4c5.sqool.ast.UnaryExpression;
import io.github.e4c5.sqool.ast.UnaryOperator;
import io.github.e4c5.sqool.core.DiagnosticSeverity;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.core.SyntaxDiagnostic;
import io.github.e4c5.sqool.grammar.mysql.generated.MySQLParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.Token;

final class MysqlAstMapper {
  private static final Pattern SUPPORTED_IDENTIFIER =
      Pattern.compile("(?i)(`[^`]+`|[a-z_][a-z0-9_$]*)(\\.(?:`[^`]+`|[a-z_][a-z0-9_$]*)){0,2}");

  private MysqlAstMapper() {}

  static ParseResult mapQueryExpression(
      MySQLParser.QueryExpressionContext context, ParseOptions options) {
    try {
      if (context.withClause() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support WITH clauses yet.", context.withClause().start);
      }
      if (!context.queryExpressionBody().queryExpressionBody().isEmpty()) {
        throw unsupportedFeature(
            "MySQL MVP does not support UNION, INTERSECT, or EXCEPT yet.",
            context.queryExpressionBody().start);
      }
      if (context.queryExpressionBody().queryExpressionParens() != null
          || context.queryExpressionBody().queryPrimary() == null
          || context.queryExpressionBody().queryPrimary().querySpecification() == null) {
        throw unsupportedFeature(
            "MySQL MVP currently supports only direct SELECT query specifications.",
            context.queryExpressionBody().start);
      }

      var querySpecification = context.queryExpressionBody().queryPrimary().querySpecification();
      var mappedBody = mapQuerySpecification(querySpecification, options);
      var orderBy = mapOrderBy(context.orderClause(), options);
      var limit = mapLimitClause(context.limitClause(), options);

      return new ParseSuccess(
          SqlDialect.MYSQL,
          new SelectStatement(
              mappedBody.selectItems(),
              mappedBody.from(),
              mappedBody.where(),
              orderBy,
              limit,
              span(context.start, context.stop, options)),
          List.of());
    } catch (UnsupportedFeatureException exception) {
      return new ParseFailure(
          SqlDialect.MYSQL,
          List.of(
              new SyntaxDiagnostic(
                  DiagnosticSeverity.ERROR,
                  exception.getMessage(),
                  exception.token == null ? 1 : exception.token.getLine(),
                  exception.token == null ? 0 : exception.token.getCharPositionInLine(),
                  exception.token == null ? null : exception.token.getText())));
    }
  }

  private static QuerySpecificationMapping mapQuerySpecification(
      MySQLParser.QuerySpecificationContext context, ParseOptions options) {
    if (context.groupByClause() != null
        || context.havingClause() != null
        || context.windowClause() != null
        || context.qualifyClause() != null
        || context.intoClause() != null
        || !context.selectOption().isEmpty()) {
      throw unsupportedFeature(
          "MySQL MVP currently supports SELECT queries without DISTINCT, GROUP BY, HAVING, or window clauses.",
          context.start);
    }

    if (context.fromClause() == null) {
      throw unsupportedFeature("MySQL MVP currently requires a FROM clause.", context.start);
    }

    return new QuerySpecificationMapping(
        mapSelectItems(context.selectItemList(), options),
        mapFromClause(context.fromClause(), options),
        context.whereClause() == null ? null : mapExpr(context.whereClause().expr(), options));
  }

  private static List<SelectItem> mapSelectItems(
      MySQLParser.SelectItemListContext context, ParseOptions options) {
    var result = new ArrayList<SelectItem>();
    if (context.MULT_OPERATOR() != null) {
      result.add(
          new AllColumnsSelectItem(
              null,
              span(
                  context.MULT_OPERATOR().getSymbol(),
                  context.MULT_OPERATOR().getSymbol(),
                  options)));
    }

    for (var selectItem : context.selectItem()) {
      if (selectItem.tableWild() != null) {
        var tableWildText = selectItem.tableWild().getText();
        result.add(
            new AllColumnsSelectItem(
                tableWildText.substring(0, tableWildText.length() - 2),
                span(selectItem.start, selectItem.stop, options)));
        continue;
      }

      if (selectItem.expr() == null) {
        throw unsupportedFeature(
            "MySQL MVP encountered an unsupported select item.", selectItem.start);
      }

      result.add(
          new ExpressionSelectItem(
              mapExpr(selectItem.expr(), options),
              aliasText(selectItem.selectAlias()),
              span(selectItem.start, selectItem.stop, options)));
    }

    return List.copyOf(result);
  }

  private static TableReference mapFromClause(
      MySQLParser.FromClauseContext context, ParseOptions options) {
    if (context.DUAL_SYMBOL() != null) {
      return new NamedTableReference(
          context.DUAL_SYMBOL().getText(),
          null,
          span(context.DUAL_SYMBOL().getSymbol(), context.DUAL_SYMBOL().getSymbol(), options));
    }

    if (context.tableReferenceList() == null
        || context.tableReferenceList().tableReference().size() != 1) {
      throw unsupportedFeature(
          "MySQL MVP currently supports exactly one FROM item or a single joined table expression.",
          context.start);
    }

    return mapTableReference(context.tableReferenceList().tableReference().getFirst(), options);
  }

  private static TableReference mapTableReference(
      MySQLParser.TableReferenceContext context, ParseOptions options) {
    if (context.tableFactor() == null) {
      throw unsupportedFeature(
          "MySQL MVP does not support ODBC-style table references.", context.start);
    }

    TableReference current = mapTableFactor(context.tableFactor(), options);
    for (var joinedTable : context.joinedTable()) {
      current = mapJoinedTable(current, joinedTable, options);
    }
    return current;
  }

  private static TableReference mapTableFactor(
      MySQLParser.TableFactorContext context, ParseOptions options) {
    if (context.singleTable() != null) {
      return mapSingleTable(context.singleTable(), options);
    }
    throw unsupportedFeature(
        "MySQL MVP currently supports only direct table references in FROM and JOIN clauses.",
        context.start);
  }

  private static NamedTableReference mapSingleTable(
      MySQLParser.SingleTableContext context, ParseOptions options) {
    if (context.usePartition() != null
        || context.indexHintList() != null
        || context.tablesampleClause() != null
        || context.tableRef() == null) {
      throw unsupportedFeature(
          "MySQL MVP currently supports only simple named tables without partitions, hints, or sampling.",
          context.start);
    }

    var tableName = context.tableRef().getText();
    if (!SUPPORTED_IDENTIFIER.matcher(tableName).matches()) {
      throw unsupportedFeature(
          "MySQL MVP encountered an unsupported table reference.", context.start);
    }

    return new NamedTableReference(
        tableName, aliasText(context.tableAlias()), span(context.start, context.stop, options));
  }

  private static JoinTableReference mapJoinedTable(
      TableReference left, MySQLParser.JoinedTableContext context, ParseOptions options) {
    if (context.identifierListWithParentheses() != null) {
      throw unsupportedFeature("MySQL MVP does not support USING join clauses yet.", context.start);
    }
    if (context.naturalJoinType() != null || context.tableFactor() != null) {
      throw unsupportedFeature("MySQL MVP does not support NATURAL joins yet.", context.start);
    }
    if (context.tableReference() == null) {
      throw unsupportedFeature("MySQL MVP encountered an unsupported join target.", context.start);
    }

    return new JoinTableReference(
        left,
        mapJoinType(context),
        mapTableReference(context.tableReference(), options),
        context.expr() == null ? null : mapExpr(context.expr(), options),
        span(context.start, context.stop, options));
  }

  private static JoinType mapJoinType(MySQLParser.JoinedTableContext context) {
    if (context.innerJoinType() != null) {
      var text = context.innerJoinType().getText().toUpperCase(Locale.ROOT);
      if (text.contains("STRAIGHT_JOIN")) {
        return JoinType.STRAIGHT;
      }
      if (text.contains("CROSS")) {
        return JoinType.CROSS;
      }
      return JoinType.INNER;
    }
    if (context.outerJoinType() != null) {
      var text = context.outerJoinType().getText().toUpperCase(Locale.ROOT);
      if (text.startsWith("LEFT")) {
        return JoinType.LEFT;
      }
      if (text.startsWith("RIGHT")) {
        return JoinType.RIGHT;
      }
    }
    throw unsupportedFeature("MySQL MVP encountered an unsupported join type.", context.start);
  }

  private static List<OrderByItem> mapOrderBy(
      MySQLParser.OrderClauseContext context, ParseOptions options) {
    if (context == null) {
      return List.of();
    }

    var result = new ArrayList<OrderByItem>();
    for (var orderExpression : context.orderList().orderExpression()) {
      result.add(
          new OrderByItem(
              mapExpr(orderExpression.expr(), options),
              orderExpression.direction() != null
                      && "DESC".equalsIgnoreCase(orderExpression.direction().getText())
                  ? SortDirection.DESC
                  : SortDirection.ASC,
              span(orderExpression.start, orderExpression.stop, options)));
    }
    return List.copyOf(result);
  }

  private static LimitClause mapLimitClause(
      MySQLParser.LimitClauseContext context, ParseOptions options) {
    if (context == null) {
      return null;
    }

    var limitOptions = context.limitOptions().limitOption();
    if (limitOptions.isEmpty() || limitOptions.size() > 2) {
      throw unsupportedFeature("MySQL MVP encountered an unsupported LIMIT shape.", context.start);
    }

    long first = numericLimit(limitOptions.getFirst());
    if (limitOptions.size() == 1) {
      return new LimitClause(first, null, span(context.start, context.stop, options));
    }

    long second = numericLimit(limitOptions.get(1));
    Long offset = context.limitOptions().OFFSET_SYMBOL() != null ? second : first;
    long rowCount = context.limitOptions().OFFSET_SYMBOL() != null ? first : second;
    return new LimitClause(rowCount, offset, span(context.start, context.stop, options));
  }

  private static long numericLimit(MySQLParser.LimitOptionContext context) {
    if (context.identifier() != null || context.PARAM_MARKER() != null) {
      throw unsupportedFeature(
          "MySQL MVP currently supports only numeric LIMIT values.", context.start);
    }
    try {
      return Long.parseLong(context.getText());
    } catch (NumberFormatException exception) {
      throw unsupportedFeature(
          "MySQL MVP encountered a LIMIT value outside the supported numeric range.",
          context.start);
    }
  }

  private static io.github.e4c5.sqool.ast.Expression mapExpr(
      MySQLParser.ExprContext context, ParseOptions options) {
    if (context instanceof MySQLParser.ExprAndContext andContext) {
      return new BinaryExpression(
          mapExpr(andContext.expr(0), options),
          BinaryOperator.AND,
          mapExpr(andContext.expr(1), options),
          span(andContext.start, andContext.stop, options));
    }
    if (context instanceof MySQLParser.ExprOrContext orContext) {
      return new BinaryExpression(
          mapExpr(orContext.expr(0), options),
          BinaryOperator.OR,
          mapExpr(orContext.expr(1), options),
          span(orContext.start, orContext.stop, options));
    }
    if (context instanceof MySQLParser.ExprNotContext notContext) {
      return new UnaryExpression(
          UnaryOperator.NOT,
          mapExpr(notContext.expr(), options),
          span(notContext.start, notContext.stop, options));
    }
    if (context instanceof MySQLParser.ExprIsContext isContext) {
      if (isContext.type != null || isContext.notRule() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support IS TRUE/FALSE/UNKNOWN yet.", isContext.start);
      }
      return mapBoolPri(isContext.boolPri(), options);
    }
    throw unsupportedFeature(
        "MySQL MVP encountered an unsupported expression form.", context.start);
  }

  private static io.github.e4c5.sqool.ast.Expression mapBoolPri(
      MySQLParser.BoolPriContext context, ParseOptions options) {
    if (context instanceof MySQLParser.PrimaryExprPredicateContext predicateContext) {
      return mapPredicate(predicateContext.predicate(), options);
    }
    if (context instanceof MySQLParser.PrimaryExprCompareContext compareContext) {
      return new BinaryExpression(
          mapBoolPri(compareContext.boolPri(), options),
          mapComparisonOperator(compareContext.compOp()),
          mapPredicate(compareContext.predicate(), options),
          span(compareContext.start, compareContext.stop, options));
    }
    if (context instanceof MySQLParser.PrimaryExprIsNullContext isNullContext) {
      return new BinaryExpression(
          mapBoolPri(isNullContext.boolPri(), options),
          isNullContext.notRule() == null ? BinaryOperator.EQUAL : BinaryOperator.NOT_EQUAL,
          new LiteralExpression("NULL", span(isNullContext.start, isNullContext.stop, options)),
          span(isNullContext.start, isNullContext.stop, options));
    }
    throw unsupportedFeature("MySQL MVP encountered an unsupported predicate form.", context.start);
  }

  private static BinaryOperator mapComparisonOperator(MySQLParser.CompOpContext context) {
    return switch (context.getText()) {
      case "=" -> BinaryOperator.EQUAL;
      case ">=" -> BinaryOperator.GREATER_OR_EQUAL;
      case ">" -> BinaryOperator.GREATER_THAN;
      case "<=" -> BinaryOperator.LESS_OR_EQUAL;
      case "<" -> BinaryOperator.LESS_THAN;
      case "!=", "<>" -> BinaryOperator.NOT_EQUAL;
      default ->
          throw unsupportedFeature(
              "MySQL MVP encountered an unsupported comparison operator.", context.start);
    };
  }

  private static io.github.e4c5.sqool.ast.Expression mapPredicate(
      MySQLParser.PredicateContext context, ParseOptions options) {
    if (context.predicateOperations() != null
        || context.simpleExprWithParentheses() != null
        || context.SOUNDS_SYMBOL() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support IN, BETWEEN, LIKE, REGEXP, or SOUNDS LIKE predicates yet.",
          context.start);
    }
    return mapBitExpr(context.bitExpr(0), options);
  }

  private static io.github.e4c5.sqool.ast.Expression mapBitExpr(
      MySQLParser.BitExprContext context, ParseOptions options) {
    if (context.op != null || !context.bitExpr().isEmpty() || context.expr() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support arithmetic or bitwise expressions yet.", context.start);
    }
    return mapSimpleExpr(context.simpleExpr(), options);
  }

  private static io.github.e4c5.sqool.ast.Expression mapSimpleExpr(
      MySQLParser.SimpleExprContext context, ParseOptions options) {
    if (context instanceof MySQLParser.SimpleExprColumnRefContext columnRefContext) {
      if (columnRefContext.jsonOperator() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support JSON column operators yet.", columnRefContext.start);
      }
      return new IdentifierExpression(
          columnRefContext.columnRef().getText(),
          span(columnRefContext.start, columnRefContext.stop, options));
    }
    if (context instanceof MySQLParser.SimpleExprLiteralContext literalContext) {
      return new LiteralExpression(
          literalContext.literalOrNull().getText(),
          span(literalContext.start, literalContext.stop, options));
    }
    if (context instanceof MySQLParser.SimpleExprUnaryContext unaryContext) {
      if (unaryContext.BITWISE_NOT_OPERATOR() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support bitwise NOT expressions yet.", unaryContext.start);
      }
      return new UnaryExpression(
          unaryContext.MINUS_OPERATOR() != null ? UnaryOperator.MINUS : UnaryOperator.PLUS,
          mapSimpleExpr(unaryContext.simpleExpr(), options),
          span(unaryContext.start, unaryContext.stop, options));
    }
    if (context instanceof MySQLParser.SimpleExprNotContext notContext) {
      return new UnaryExpression(
          UnaryOperator.NOT,
          mapSimpleExpr(notContext.simpleExpr(), options),
          span(notContext.start, notContext.stop, options));
    }
    if (context instanceof MySQLParser.SimpleExprListContext listContext) {
      if (listContext.ROW_SYMBOL() != null || listContext.exprList().expr().size() != 1) {
        throw unsupportedFeature(
            "MySQL MVP does not support row constructors or expression lists yet.",
            listContext.start);
      }
      return mapExpr(listContext.exprList().expr().getFirst(), options);
    }
    throw unsupportedFeature(
        "MySQL MVP encountered an unsupported expression leaf.", context.start);
  }

  private static String aliasText(MySQLParser.SelectAliasContext context) {
    if (context == null) {
      return null;
    }
    if (context.identifier() != null) {
      return context.identifier().getText();
    }
    if (context.textStringLiteral() != null) {
      return context.textStringLiteral().getText();
    }
    return context.getText();
  }

  private static String aliasText(MySQLParser.TableAliasContext context) {
    return context == null || context.identifier() == null ? null : context.identifier().getText();
  }

  private static UnsupportedFeatureException unsupportedFeature(String message, Token token) {
    return new UnsupportedFeatureException(message, token);
  }

  private static SourceSpan span(Token start, Token stop, ParseOptions options) {
    if (!options.includeSourceSpans() || start == null || stop == null) {
      return null;
    }

    int stopColumn = stop.getCharPositionInLine();
    if (stop.getText() != null && !stop.getText().isEmpty()) {
      stopColumn += stop.getText().length() - 1;
    }

    return new SourceSpan(
        start.getStartIndex(),
        stop.getStopIndex(),
        start.getLine(),
        start.getCharPositionInLine(),
        stop.getLine(),
        stopColumn);
  }

  private record QuerySpecificationMapping(
      List<SelectItem> selectItems,
      TableReference from,
      io.github.e4c5.sqool.ast.Expression where) {}

  private static final class UnsupportedFeatureException extends RuntimeException {
    private final Token token;

    private UnsupportedFeatureException(String message, Token token) {
      super(message);
      this.token = token;
    }
  }
}
