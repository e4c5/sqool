package io.github.e4c5.sqool.dialect.mysql;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.BetweenExpression;
import io.github.e4c5.sqool.ast.BinaryExpression;
import io.github.e4c5.sqool.ast.BinaryOperator;
import io.github.e4c5.sqool.ast.Expression;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.FunctionCallExpression;
import io.github.e4c5.sqool.ast.IdentifierExpression;
import io.github.e4c5.sqool.ast.InExpression;
import io.github.e4c5.sqool.ast.JoinTableReference;
import io.github.e4c5.sqool.ast.JoinType;
import io.github.e4c5.sqool.ast.LikeExpression;
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
              mappedBody.distinct(),
              mappedBody.selectItems(),
              mappedBody.from(),
              mappedBody.where(),
              mappedBody.groupBy(),
              mappedBody.having(),
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
    if (context.windowClause() != null
        || context.qualifyClause() != null
        || context.intoClause() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support window, QUALIFY, or INTO clauses yet.", context.start);
    }

    if (context.fromClause() == null) {
      throw unsupportedFeature("MySQL MVP currently requires a FROM clause.", context.start);
    }

    return new QuerySpecificationMapping(
        mapDistinct(context.selectOption()),
        mapSelectItems(context.selectItemList(), options),
        mapFromClause(context.fromClause(), options),
        context.whereClause() == null ? null : mapExpr(context.whereClause().expr(), options),
        mapGroupBy(context.groupByClause(), options),
        context.havingClause() == null ? null : mapExpr(context.havingClause().expr(), options));
  }

  private static boolean mapDistinct(List<MySQLParser.SelectOptionContext> selectOptions) {
    boolean distinct = false;
    for (var selectOption : selectOptions) {
      if (selectOption.querySpecOption() == null) {
        throw unsupportedFeature(
            "MySQL MVP does not support SQL_NO_CACHE select options yet.", selectOption.start);
      }
      String option = selectOption.querySpecOption().getText().toUpperCase(Locale.ROOT);
      switch (option) {
        case "DISTINCT" -> distinct = true;
        case "ALL" -> {}
        default ->
            throw unsupportedFeature(
                "MySQL MVP does not support select option '" + option + "' yet.",
                selectOption.start);
      }
    }
    return distinct;
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

  private static List<Expression> mapGroupBy(
      MySQLParser.GroupByClauseContext context, ParseOptions options) {
    if (context == null) {
      return List.of();
    }
    if (context.groupList() != null || context.olapOption() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support ROLLUP, CUBE, or advanced GROUP BY options yet.",
          context.start);
    }

    var result = new ArrayList<Expression>();
    for (var orderExpression : context.orderList().orderExpression()) {
      if (orderExpression.direction() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support sort directions inside GROUP BY yet.",
            orderExpression.start);
      }
      result.add(mapExpr(orderExpression.expr(), options));
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

  private static Expression mapExpr(MySQLParser.ExprContext context, ParseOptions options) {
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

  private static Expression mapBoolPri(MySQLParser.BoolPriContext context, ParseOptions options) {
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

  private static Expression mapPredicate(
      MySQLParser.PredicateContext context, ParseOptions options) {
    if (context.simpleExprWithParentheses() != null || context.SOUNDS_SYMBOL() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support MEMBER OF or SOUNDS LIKE predicates yet.", context.start);
    }
    Expression left = mapBitExpr(context.bitExpr(0), options);
    if (context.predicateOperations() == null) {
      return left;
    }

    boolean negated = context.notRule() != null;
    if (context.predicateOperations() instanceof MySQLParser.PredicateExprInContext inContext) {
      if (inContext.subquery() != null) {
        throw unsupportedFeature("MySQL MVP does not support IN subqueries yet.", inContext.start);
      }
      return new InExpression(
          left,
          mapExprList(inContext.exprList(), options),
          negated,
          span(inContext.start, inContext.stop, options));
    }
    if (context.predicateOperations()
        instanceof MySQLParser.PredicateExprBetweenContext betweenContext) {
      return new BetweenExpression(
          left,
          mapBitExpr(betweenContext.bitExpr(), options),
          mapPredicate(betweenContext.predicate(), options),
          negated,
          span(betweenContext.start, betweenContext.stop, options));
    }
    if (context.predicateOperations() instanceof MySQLParser.PredicateExprLikeContext likeContext) {
      return new LikeExpression(
          left,
          mapSimpleExpr(likeContext.simpleExpr(0), options),
          likeContext.simpleExpr().size() > 1
              ? mapSimpleExpr(likeContext.simpleExpr(1), options)
              : null,
          negated,
          span(likeContext.start, likeContext.stop, options));
    }
    throw unsupportedFeature(
        "MySQL MVP does not support this predicate operation yet.",
        context.predicateOperations().start);
  }

  private static Expression mapBitExpr(MySQLParser.BitExprContext context, ParseOptions options) {
    if (context.expr() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support INTERVAL arithmetic yet.", context.start);
    }
    if (!context.bitExpr().isEmpty()) {
      if (context.op == null) {
        throw unsupportedFeature(
            "MySQL MVP encountered an unsupported arithmetic form.", context.start);
      }
      return new BinaryExpression(
          mapBitExpr(context.bitExpr(0), options),
          mapArithmeticOperator(context.op),
          mapBitExpr(context.bitExpr(1), options),
          span(context.start, context.stop, options));
    }
    return mapSimpleExpr(context.simpleExpr(), options);
  }

  private static BinaryOperator mapArithmeticOperator(Token operatorToken) {
    return switch (operatorToken.getText().toUpperCase(Locale.ROOT)) {
      case "+" -> BinaryOperator.PLUS;
      case "-" -> BinaryOperator.MINUS;
      case "*" -> BinaryOperator.MULTIPLY;
      case "/", "DIV" -> BinaryOperator.DIVIDE;
      case "%", "MOD" -> BinaryOperator.MODULO;
      default ->
          throw unsupportedFeature(
              "MySQL MVP does not support arithmetic operator '"
                  + operatorToken.getText()
                  + "' yet.",
              operatorToken);
    };
  }

  private static Expression mapSimpleExpr(
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
    if (context instanceof MySQLParser.SimpleExprFunctionContext functionContext) {
      return mapFunctionCall(functionContext.functionCall(), options);
    }
    if (context instanceof MySQLParser.SimpleExprSumContext sumContext) {
      return mapSumExpr(sumContext.sumExpr(), options);
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
    if (context instanceof MySQLParser.SimpleExprRuntimeFunctionContext runtimeContext) {
      return mapRuntimeFunction(runtimeContext.runtimeFunctionCall(), options);
    }
    throw unsupportedFeature(
        "MySQL MVP encountered an unsupported expression leaf.", context.start);
  }

  private static FunctionCallExpression mapRuntimeFunction(
      MySQLParser.RuntimeFunctionCallContext context, ParseOptions options) {
    if (context.COALESCE_SYMBOL() != null && context.exprListWithParentheses() != null) {
      return functionCall(
          "COALESCE",
          mapExprList(context.exprListWithParentheses().exprList(), options),
          span(context.start, context.stop, options));
    }
    if (context.IF_SYMBOL() != null && context.expr().size() == 3) {
      return functionCall(
          "IF",
          List.of(
              mapExpr(context.expr(0), options),
              mapExpr(context.expr(1), options),
              mapExpr(context.expr(2), options)),
          span(context.start, context.stop, options));
    }
    if (context.MOD_SYMBOL() != null && context.expr().size() == 2) {
      return functionCall(
          "MOD",
          List.of(mapExpr(context.expr(0), options), mapExpr(context.expr(1), options)),
          span(context.start, context.stop, options));
    }
    if (context.DATE_SYMBOL() != null && context.exprWithParentheses() != null) {
      return functionCall(
          "DATE",
          List.of(mapExpr(context.exprWithParentheses().expr(), options)),
          span(context.start, context.stop, options));
    }
    if (context.NOW_SYMBOL() != null) {
      return functionCall(
          "NOW",
          mapTimeFunctionArguments(context.timeFunctionParameters(), options),
          span(context.start, context.stop, options));
    }
    if (context.CURDATE_SYMBOL() != null) {
      return functionCall("CURDATE", List.of(), span(context.start, context.stop, options));
    }
    if (context.CURRENT_USER_SYMBOL() != null) {
      return functionCall("CURRENT_USER", List.of(), span(context.start, context.stop, options));
    }
    throw unsupportedFeature(
        "MySQL MVP does not support built-in runtime function '"
            + context.getStart().getText()
            + "' yet.",
        context.start);
  }

  private static Expression mapFunctionCall(
      MySQLParser.FunctionCallContext context, ParseOptions options) {
    String name =
        context.pureIdentifier() != null
            ? context.pureIdentifier().getText()
            : context.qualifiedIdentifier().getText();
    List<Expression> arguments =
        context.udfExprList() != null
            ? mapUdfExprList(context.udfExprList(), options)
            : context.exprList() != null ? mapExprList(context.exprList(), options) : List.of();
    return new FunctionCallExpression(
        name, arguments, false, false, span(context.start, context.stop, options));
  }

  private static List<Expression> mapUdfExprList(
      MySQLParser.UdfExprListContext context, ParseOptions options) {
    var arguments = new ArrayList<Expression>();
    for (var udfExpr : context.udfExpr()) {
      if (udfExpr.selectAlias() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support aliased function arguments yet.", udfExpr.start);
      }
      arguments.add(mapExpr(udfExpr.expr(), options));
    }
    return List.copyOf(arguments);
  }

  private static FunctionCallExpression mapSumExpr(
      MySQLParser.SumExprContext context, ParseOptions options) {
    if (context.windowingClause() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support windowed aggregate functions yet.", context.start);
    }
    if (context.jsonFunction() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support JSON aggregate functions yet.", context.start);
    }
    if (context.GROUP_CONCAT_SYMBOL() != null
        && (context.orderClause() != null || context.SEPARATOR_SYMBOL() != null)) {
      throw unsupportedFeature(
          "MySQL MVP does not support GROUP_CONCAT ORDER BY or SEPARATOR clauses yet.",
          context.start);
    }

    String name = context.name == null ? context.getStart().getText() : context.name.getText();
    boolean distinct = context.DISTINCT_SYMBOL() != null;
    boolean starArgument = context.MULT_OPERATOR() != null;

    List<Expression> arguments =
        context.exprList() != null
            ? mapExprList(context.exprList(), options)
            : context.inSumExpr() != null
                ? List.of(mapExpr(context.inSumExpr().expr(), options))
                : List.of();

    return new FunctionCallExpression(
        name, arguments, distinct, starArgument, span(context.start, context.stop, options));
  }

  private static FunctionCallExpression functionCall(
      String name, List<Expression> arguments, SourceSpan sourceSpan) {
    return new FunctionCallExpression(name, arguments, false, false, sourceSpan);
  }

  private static List<Expression> mapExprList(
      MySQLParser.ExprListContext context, ParseOptions options) {
    var expressions = new ArrayList<Expression>();
    for (var expr : context.expr()) {
      expressions.add(mapExpr(expr, options));
    }
    return List.copyOf(expressions);
  }

  private static List<Expression> mapTimeFunctionArguments(
      MySQLParser.TimeFunctionParametersContext context, ParseOptions options) {
    if (context == null || context.fractionalPrecision() == null) {
      return List.of();
    }
    return List.of(
        new LiteralExpression(
            context.fractionalPrecision().getText(),
            span(
                context.fractionalPrecision().start, context.fractionalPrecision().stop, options)));
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
      boolean distinct,
      List<SelectItem> selectItems,
      TableReference from,
      Expression where,
      List<Expression> groupBy,
      Expression having) {}

  private static final class UnsupportedFeatureException extends RuntimeException {
    private final Token token;

    private UnsupportedFeatureException(String message, Token token) {
      super(message);
      this.token = token;
    }
  }
}
