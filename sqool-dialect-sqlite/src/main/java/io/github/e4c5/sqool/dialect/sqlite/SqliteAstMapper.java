package io.github.e4c5.sqool.dialect.sqlite;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.Expression;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
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
import io.github.e4c5.sqool.ast.SqlScript;
import io.github.e4c5.sqool.ast.SqliteRawStatement;
import io.github.e4c5.sqool.ast.SqliteStatementKind;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.ast.TableReference;
import io.github.e4c5.sqool.ast.UnaryExpression;
import io.github.e4c5.sqool.ast.UnaryOperator;
import io.github.e4c5.sqool.core.ParseMetrics;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SourceSpans;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.grammar.sqlite.generated.SQLiteLexer;
import io.github.e4c5.sqool.grammar.sqlite.generated.SQLiteParser;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

final class SqliteAstMapper {

  private SqliteAstMapper() {}

  static ParseResult mapSqlStmtList(
      SQLiteParser.Sql_stmt_listContext stmtList, ParseOptions options) {
    List<Statement> statements = new ArrayList<>();
    for (SQLiteParser.Sql_stmtContext stmt : stmtList.sql_stmt()) {
      ParseResult one = mapSqlStmt(stmt, options);
      if (one instanceof io.github.e4c5.sqool.core.ParseFailure) {
        return one;
      }
      statements.add((Statement) ((ParseSuccess) one).root());
    }
    SourceSpan scriptSpan =
        statements.isEmpty()
            ? null
            : SourceSpans.fromTokens(
                stmtList.sql_stmt(0).start, stmtList.sql_stmt(statements.size() - 1).stop, options);
    return new ParseSuccess(
        SqlDialect.SQLITE,
        new SqlScript(statements, scriptSpan),
        List.of(),
        ParseMetrics.unknown());
  }

  static ParseResult mapSqlStmt(SQLiteParser.Sql_stmtContext stmt, ParseOptions options) {
    if (stmt.select_stmt() != null) {
      return mapSelectStmt(stmt.select_stmt(), options);
    }
    SqliteStatementKind kind = kindForSqlStmt(stmt);
    String sqlText = textOf(stmt);
    return new ParseSuccess(
        SqlDialect.SQLITE,
        new SqliteRawStatement(
            kind, sqlText, SourceSpans.fromTokens(stmt.start, stmt.stop, options)),
        List.of(),
        ParseMetrics.unknown());
  }

  private static ParseResult mapSelectStmt(
      SQLiteParser.Select_stmtContext context, ParseOptions options) {
    if (hasUnsupportedSelectShape(context)) {
      return rawSelect(context, options);
    }
    SQLiteParser.Select_coreContext core = context.select_core(0);

    boolean distinct = core.DISTINCT_() != null;

    MappingResult<List<SelectItem>> selectItemsResult =
        mapResultColumns(core.result_column(), options);
    if (!selectItemsResult.supported()) {
      return rawSelect(context, options);
    }

    TableReference from = core.FROM_() != null ? mapJoinClause(core.join_clause(), options) : null;
    if (core.FROM_() != null && from == null) {
      // FROM clause present but not representable in the current AST slice.
      return rawSelect(context, options);
    }

    MappingResult<Expression> whereResult = mapWhereExpression(core, options);
    if (!whereResult.supported()) {
      return rawSelect(context, options);
    }
    Expression where = whereResult.value();

    MappingResult<List<Expression>> groupByResult = mapGroupByExpressions(core, options);
    if (!groupByResult.supported()) {
      return rawSelect(context, options);
    }
    List<Expression> groupBy = groupByResult.value();

    MappingResult<Expression> havingResult = mapHavingExpression(core, options);
    if (!havingResult.supported()) {
      return rawSelect(context, options);
    }
    Expression having = havingResult.value();

    MappingResult<List<OrderByItem>> orderByResult =
        mapOrderClause(context.order_clause(), options);
    if (!orderByResult.supported()) {
      return rawSelect(context, options);
    }
    List<OrderByItem> orderBy = orderByResult.value();

    LimitClause limit = null;
    if (context.limit_clause() != null) {
      limit = mapLimitClause(context.limit_clause(), options);
      if (limit == null) {
        return rawSelect(context, options);
      }
    }

    return new ParseSuccess(
        SqlDialect.SQLITE,
        new SelectStatement(
            distinct,
            selectItemsResult.value(),
            from,
            where,
            groupBy,
            having,
            orderBy,
            limit,
            SourceSpans.fromTokens(context.start, context.stop, options)),
        List.of(),
        ParseMetrics.unknown());
  }

  private static ParseResult rawSelect(
      SQLiteParser.Select_stmtContext context, ParseOptions options) {
    return new ParseSuccess(
        SqlDialect.SQLITE,
        new SqliteRawStatement(
            SqliteStatementKind.SELECT,
            textOf(context),
            SourceSpans.fromTokens(context.start, context.stop, options)),
        List.of(),
        ParseMetrics.unknown());
  }

  private static MappingResult<List<SelectItem>> mapResultColumns(
      List<SQLiteParser.Result_columnContext> columns, ParseOptions options) {
    List<SelectItem> items = new ArrayList<>();
    for (SQLiteParser.Result_columnContext col : columns) {
      if (col.STAR() != null) {
        if (col.table_name() != null) {
          items.add(
              new AllColumnsSelectItem(
                  col.table_name().getText(),
                  SourceSpans.fromTokens(col.start, col.stop, options)));
        } else {
          items.add(
              new AllColumnsSelectItem(null, SourceSpans.fromTokens(col.start, col.stop, options)));
        }
      } else {
        Expression expr = mapExpr(col.expr(), options);
        if (expr == null) {
          // Projection expression is outside the supported subset.
          return new MappingResult<>(false, List.of());
        }
        String alias = col.column_alias() != null ? col.column_alias().getText() : null;
        items.add(
            new ExpressionSelectItem(
                expr, alias, SourceSpans.fromTokens(col.start, col.stop, options)));
      }
    }
    return new MappingResult<>(true, List.copyOf(items));
  }

  private static TableReference mapJoinClause(
      SQLiteParser.Join_clauseContext context, ParseOptions options) {
    if (context == null) {
      return null;
    }

    List<SQLiteParser.Table_or_subqueryContext> tables = context.table_or_subquery();
    if (tables.isEmpty()) {
      return null;
    }

    TableReference current = mapTableOrSubquery(tables.get(0), options);
    if (current == null) {
      return null;
    }

    List<SQLiteParser.Join_operatorContext> operators = context.join_operator();
    List<SQLiteParser.Join_constraintContext> constraints = context.join_constraint();
    // join_constraint is optional per step; when counts differ we cannot map constraint i to step i.
    if (constraints.size() != operators.size()) {
      return null;
    }

    for (int i = 0; i < operators.size(); i++) {
      JoinType joinType = mapJoinOperator(operators.get(i));
      if (joinType == null) {
        return null;
      }

      TableReference nextTable = mapTableOrSubquery(tables.get(i + 1), options);
      if (nextTable == null) {
        return null;
      }

      SQLiteParser.Join_constraintContext constraint =
          i < constraints.size() ? constraints.get(i) : null;
      JoinStepContext stepContext =
          new JoinStepContext(constraint, operators.get(i), context, i + 1);
      JoinTableReference nextRef =
          buildJoinTableReference(current, joinType, nextTable, stepContext, options);
      if (nextRef == null) {
        return null;
      }
      current = nextRef;
    }

    return current;
  }

  /** Builds one JoinTableReference from left, operator, right and optional constraint. */
  private static JoinTableReference buildJoinTableReference(
      TableReference left,
      JoinType joinType,
      TableReference right,
      JoinStepContext step,
      ParseOptions options) {
    Expression condition = null;
    List<String> usingColumns = List.of();
    if (step.constraint() != null) {
      if (step.constraint().ON_() != null) {
        condition = mapExpr(step.constraint().expr(), options);
        if (condition == null) {
          return null;
        }
      } else if (step.constraint().USING_() != null) {
        usingColumns =
            step.constraint().column_name().stream()
                .map(SQLiteParser.Column_nameContext::getText)
                .toList();
      }
    }
    boolean natural = step.operatorContext().NATURAL_() != null;
    return new JoinTableReference(
        left,
        joinType,
        right,
        condition,
        usingColumns,
        natural,
        SourceSpans.fromTokens(
            step.joinClause().start,
            step.joinClause().table_or_subquery().get(step.rightTableIndex()).stop,
            options));
  }

  private record JoinStepContext(
      SQLiteParser.Join_constraintContext constraint,
      SQLiteParser.Join_operatorContext operatorContext,
      SQLiteParser.Join_clauseContext joinClause,
      int rightTableIndex) {}

  private static io.github.e4c5.sqool.ast.JoinType mapJoinOperator(
      SQLiteParser.Join_operatorContext context) {
    if (context.COMMA() != null) {
      return io.github.e4c5.sqool.ast.JoinType.INNER;
    }

    // NATURAL JOIN is normalized; the caller handles the NATURAL flag.
    if (context.NATURAL_() != null) {
      boolean hasLeft = context.LEFT_() != null;
      boolean hasRight = context.RIGHT_() != null;
      boolean hasFull = context.FULL_() != null;
      return io.github.e4c5.sqool.ast.JoinType.fromKind(hasLeft, hasRight, hasFull);
    }

    boolean hasLeft = context.LEFT_() != null;
    boolean hasRight = context.RIGHT_() != null;
    boolean hasFull = context.FULL_() != null;
    boolean hasInner = context.INNER_() != null;
    boolean hasCross = context.CROSS_() != null;

    if (hasCross) return io.github.e4c5.sqool.ast.JoinType.CROSS;
    if (hasInner) return io.github.e4c5.sqool.ast.JoinType.INNER;
    return io.github.e4c5.sqool.ast.JoinType.fromKind(hasLeft, hasRight, hasFull);
  }

  private static TableReference mapTableOrSubquery(
      SQLiteParser.Table_or_subqueryContext context, ParseOptions options) {
    if (context.schema_name() != null && !context.schema_name().isEmpty()) {
      // Schema-qualified tables are not yet supported in the normalized AST.
      return null;
    }

    if (context.table_name() != null) {
      String tableName = context.table_name().getText();
      String alias = context.table_alias() != null ? context.table_alias().getText() : null;
      if (alias == null && context.table_alias_excluding_joins() != null) {
        alias = context.table_alias_excluding_joins().getText();
      }
      return new NamedTableReference(
          tableName, alias, SourceSpans.fromTokens(context.start, context.stop, options));
    }

    if (context.join_clause() != null) {
      return mapJoinClause(context.join_clause(), options);
    }

    // Subqueries, table functions, and indexed-by are not yet supported.
    return null;
  }

  private static Expression mapExpr(SQLiteParser.ExprContext context, ParseOptions options) {
    if (context == null) return null;
    return mapExprOr(context.expr_or(), options);
  }

  private static Expression mapExprOr(SQLiteParser.Expr_orContext context, ParseOptions options) {
    if (context == null || context.expr_and().isEmpty()) return null;
    Expression current = mapAnd(context.expr_and(0), options);
    if (current == null) return null;

    for (int i = 1; i < context.expr_and().size(); i++) {
      Expression rhs = mapAnd(context.expr_and(i), options);
      if (rhs == null) return null;
      current =
          new io.github.e4c5.sqool.ast.BinaryExpression(
              current,
              io.github.e4c5.sqool.ast.BinaryOperator.OR,
              rhs,
              SourceSpans.fromTokens(context.start, context.expr_and(i).stop, options));
    }
    return current;
  }

  private static Expression mapAnd(SQLiteParser.Expr_andContext context, ParseOptions options) {
    if (context == null || context.expr_not().isEmpty()) {
      return null;
    }

    Expression current = mapNot(context.expr_not(0), options);
    if (current == null) {
      return null;
    }

    for (int i = 1; i < context.expr_not().size(); i++) {
      Expression rhs = mapNot(context.expr_not(i), options);
      if (rhs == null) {
        return null;
      }
      current =
          new io.github.e4c5.sqool.ast.BinaryExpression(
              current,
              io.github.e4c5.sqool.ast.BinaryOperator.AND,
              rhs,
              SourceSpans.fromTokens(context.start, context.stop, options));
    }

    return current;
  }

  private static Expression mapNot(SQLiteParser.Expr_notContext context, ParseOptions options) {
    if (context == null) {
      return null;
    }

    Expression base = mapExprFromBinary(context.expr_binary(), options);
    if (base == null) {
      return null;
    }

    int notCount = context.NOT_() == null ? 0 : context.NOT_().size();
    if (notCount % 2 == 0) {
      return base;
    }

    return new io.github.e4c5.sqool.ast.UnaryExpression(
        io.github.e4c5.sqool.ast.UnaryOperator.NOT,
        base,
        SourceSpans.fromTokens(context.start, context.stop, options));
  }

  private static Expression mapExprFromBinary(
      SQLiteParser.Expr_binaryContext context, ParseOptions options) {
    if (context == null || context.expr_comparison().isEmpty()) return null;

    // expr_binary allows postfix (ISNULL_, NOTNULL_, NOT NULL_) with no second expr_comparison;
    // we only fold binary forms, so reject single-expr + trailing children (unsupported shape).
    if (context.expr_comparison().size() == 1 && context.getChildCount() > 1) {
      return null;
    }

    Expression current = mapExprComparison(context.expr_comparison(0), options);
    if (current == null) return null;

    // Handle (operator expr_comparison)*
    // In SQLite grammar, expr_binary has a list of expr_comparison and some operators between them.
    // We need to fold them.
    int childCount = context.getChildCount();
    for (int i = 1; i < childCount; i++) {
      var child = context.getChild(i);
      if (child instanceof SQLiteParser.Expr_comparisonContext nextCompCtx) {
        Expression rhs = mapExprComparison(nextCompCtx, options);
        if (rhs == null) return null;

        // Find the operator between current and rhs. It's the child before nextCompCtx.
        var opChild = context.getChild(i - 1);
        io.github.e4c5.sqool.ast.BinaryOperator op = mapBinaryOperator(opChild.getText());
        if (op == null) return null;

        current =
            new io.github.e4c5.sqool.ast.BinaryExpression(
                current, op, rhs, SourceSpans.fromTokens(context.start, nextCompCtx.stop, options));
      }
    }

    return current;
  }

  private static Expression mapExprComparison(
      SQLiteParser.Expr_comparisonContext context, ParseOptions options) {
    if (context == null || context.expr_bitwise().isEmpty()) return null;
    Expression current = mapBitwise(context.expr_bitwise(0), options);
    if (current == null) return null;

    for (int i = 1; i < context.expr_bitwise().size(); i++) {
      var rhsCtx = context.expr_bitwise(i);
      Expression rhs = mapBitwise(rhsCtx, options);
      if (rhs == null) return null;

      var opText = context.getChild(2 * i - 1).getText();
      var op = mapBinaryOperator(opText);
      if (op == null) return null;

      current =
          new io.github.e4c5.sqool.ast.BinaryExpression(
              current, op, rhs, SourceSpans.fromTokens(context.start, rhsCtx.stop, options));
    }
    return current;
  }

  private static Expression mapBitwise(
      SQLiteParser.Expr_bitwiseContext context, ParseOptions options) {
    if (context == null || context.expr_addition().isEmpty()) return null;
    Expression current = mapAddition(context.expr_addition(0), options);
    // Folding similar to above... but for simplicity of this task, I'll only do comparison for now
    // as it's the blocking issue for JOIN ON.
    if (context.expr_addition().size() > 1) return null;
    return current;
  }

  private static Expression mapAddition(
      SQLiteParser.Expr_additionContext context, ParseOptions options) {
    if (context == null || context.expr_multiplication().isEmpty()) return null;
    Expression current = mapMultiplication(context.expr_multiplication(0), options);
    if (context.expr_multiplication().size() > 1) return null;
    return current;
  }

  private static Expression mapMultiplication(
      SQLiteParser.Expr_multiplicationContext context, ParseOptions options) {
    if (context == null || context.expr_string().isEmpty()) return null;
    Expression current = mapStringExpr(context.expr_string(0), options);
    if (context.expr_string().size() > 1) return null;
    return current;
  }

  private static Expression mapStringExpr(
      SQLiteParser.Expr_stringContext context, ParseOptions options) {
    if (context == null || context.expr_collate().isEmpty()) return null;
    Expression current = mapCollate(context.expr_collate(0), options);
    if (context.expr_collate().size() > 1) return null;
    return current;
  }

  private static Expression mapCollate(
      SQLiteParser.Expr_collateContext context, ParseOptions options) {
    if (context == null || context.expr_unary() == null) return null;
    // Ignore COLLATE for now
    return mapUnary(context.expr_unary(), options);
  }

  private static Expression mapUnary(SQLiteParser.Expr_unaryContext context, ParseOptions options) {
    if (context == null || context.expr_base() == null) return null;

    int countMinus = 0;
    for (int i = 0; i < context.getChildCount(); i++) {
      if (context.getChild(i) instanceof SQLiteParser.Expr_baseContext) {
        break;
      }
      if (context.getChild(i) instanceof TerminalNode tn) {
        int type = tn.getSymbol().getType();
        if (type == SQLiteLexer.TILDE) {
          return null; // bitwise unary not supported
        }
        if (type == SQLiteLexer.MINUS) {
          countMinus++;
        }
        // PLUS is identity, ignore
      }
    }

    Expression base = mapBase(context.expr_base(), options);
    if (base == null) return null;

    if (countMinus % 2 == 1) {
      base =
          new UnaryExpression(
              UnaryOperator.MINUS,
              base,
              SourceSpans.fromTokens(context.start, context.stop, options));
    }
    return base;
  }

  private static Expression mapBase(SQLiteParser.Expr_baseContext context, ParseOptions options) {
    if (context.literal_value() != null) {
      return mapLiteralValue(context.literal_value(), options);
    }
    if (context.BIND_PARAMETER() != null) {
      return new LiteralExpression(
          context.getText(), SourceSpans.fromTokens(context.start, context.stop, options));
    }
    if (context.column_name_excluding_string() != null) {
      return new io.github.e4c5.sqool.ast.IdentifierExpression(
          context.column_name_excluding_string().getText(),
          SourceSpans.fromTokens(context.start, context.stop, options));
    }
    if (context.table_name() != null && context.DOT() != null && context.column_name() != null) {
      // Preserve full form (schema.table.column or table.column) from source.
      return new io.github.e4c5.sqool.ast.IdentifierExpression(
          context.getText(), SourceSpans.fromTokens(context.start, context.stop, options));
    }
    if (context.OPEN_PAR() != null && context.select_stmt() != null) {
      // Subqueries are not yet supported
      return null;
    }
    if (context.expr_recursive() != null) {
      return mapRecursiveExpr(context.expr_recursive(), options);
    }
    return new LiteralExpression(
        context.getText(), SourceSpans.fromTokens(context.start, context.stop, options));
  }

  private static Expression mapRecursiveExpr(
      SQLiteParser.Expr_recursiveContext context, ParseOptions options) {
    if (context.OPEN_PAR() != null && context.expr().size() == 1) {
      return mapExpr(context.expr(0), options);
    }
    return null;
  }

  private static io.github.e4c5.sqool.ast.BinaryOperator mapBinaryOperator(String text) {
    return switch (text.toUpperCase()) {
      case "=", "==" -> io.github.e4c5.sqool.ast.BinaryOperator.EQUAL;
      case "<>", "!=" -> io.github.e4c5.sqool.ast.BinaryOperator.NOT_EQUAL;
      case "<" -> io.github.e4c5.sqool.ast.BinaryOperator.LESS_THAN;
      case "<=" -> io.github.e4c5.sqool.ast.BinaryOperator.LESS_OR_EQUAL;
      case ">" -> io.github.e4c5.sqool.ast.BinaryOperator.GREATER_THAN;
      case ">=" -> io.github.e4c5.sqool.ast.BinaryOperator.GREATER_OR_EQUAL;
      case "+" -> io.github.e4c5.sqool.ast.BinaryOperator.PLUS;
      case "-" -> io.github.e4c5.sqool.ast.BinaryOperator.MINUS;
      case "*" -> io.github.e4c5.sqool.ast.BinaryOperator.MULTIPLY;
      case "/" -> io.github.e4c5.sqool.ast.BinaryOperator.DIVIDE;
      case "AND" -> io.github.e4c5.sqool.ast.BinaryOperator.AND;
      case "OR" -> io.github.e4c5.sqool.ast.BinaryOperator.OR;
      default -> null;
    };
  }

  private static Expression mapLiteralValue(
      SQLiteParser.Literal_valueContext context, ParseOptions options) {
    return new LiteralExpression(
        context.getText(), SourceSpans.fromTokens(context.start, context.stop, options));
  }

  private static MappingResult<List<OrderByItem>> mapOrderClause(
      SQLiteParser.Order_clauseContext context, ParseOptions options) {
    if (context == null) {
      return new MappingResult<>(true, List.of());
    }
    List<OrderByItem> items = new ArrayList<>();
    for (SQLiteParser.Ordering_termContext term : context.ordering_term()) {
      // COLLATE and NULLS modifiers are not representable in the current AST; fall back.
      if (term.COLLATE_() != null || term.NULLS_() != null) {
        return new MappingResult<>(false, List.of());
      }
      Expression expr = mapExpr(term.expr(), options);
      if (expr == null) {
        return new MappingResult<>(false, List.of());
      }
      SortDirection dir = SortDirection.ASC;
      if (term.asc_desc() != null && term.asc_desc().DESC_() != null) {
        dir = SortDirection.DESC;
      }
      items.add(new OrderByItem(expr, dir, SourceSpans.fromTokens(term.start, term.stop, options)));
    }
    return new MappingResult<>(true, List.copyOf(items));
  }

  private static LimitClause mapLimitClause(
      SQLiteParser.Limit_clauseContext context, ParseOptions options) {
    List<SQLiteParser.ExprContext> exprs = context.expr();
    if (exprs.isEmpty()) {
      return null;
    }

    Long first = parseLimitExpr(exprs.get(0));
    if (first == null) {
      return null;
    }

    Long second = null;
    if (exprs.size() > 1) {
      second = parseLimitExpr(exprs.get(1));
      if (second == null) {
        return null;
      }
    }

    Long rowCount;
    Long offset;
    if (exprs.size() == 1) {
      // LIMIT rowCount
      rowCount = first;
      offset = null;
    } else if (context.OFFSET_() != null) {
      // LIMIT rowCount OFFSET offset
      rowCount = first;
      offset = second;
    } else if (context.COMMA() != null) {
      // LIMIT offset, rowCount
      offset = first;
      rowCount = second;
    } else {
      // Unexpected shape; treat as unsupported so the caller can fall back to raw.
      return null;
    }

    return new LimitClause(
        rowCount, offset, SourceSpans.fromTokens(context.start, context.stop, options));
  }

  private static Long parseLimitExpr(SQLiteParser.ExprContext expr) {
    String text = expr.getText();
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean hasUnsupportedSelectShape(SQLiteParser.Select_stmtContext context) {
    if (context.with_clause() != null) {
      return true;
    }
    if (context.compound_operator() != null && !context.compound_operator().isEmpty()) {
      return true;
    }
    SQLiteParser.Select_coreContext core = context.select_core(0);
    if (core.values_clause() != null) {
      return true;
    }
    return core.WINDOW_() != null;
  }

  private static MappingResult<Expression> mapWhereExpression(
      SQLiteParser.Select_coreContext core, ParseOptions options) {
    if (core.where_expr == null) {
      return new MappingResult<>(true, null);
    }
    Expression where = mapExpr(core.where_expr, options);
    if (where == null) {
      return new MappingResult<>(false, null);
    }
    return new MappingResult<>(true, where);
  }

  private static MappingResult<List<Expression>> mapGroupByExpressions(
      SQLiteParser.Select_coreContext core, ParseOptions options) {
    if (core.group_by_expr == null || core.group_by_expr.isEmpty()) {
      return new MappingResult<>(true, List.of());
    }
    var grouped = new ArrayList<Expression>();
    for (SQLiteParser.ExprContext groupExpr : core.group_by_expr) {
      Expression mapped = mapExpr(groupExpr, options);
      if (mapped == null) {
        return new MappingResult<>(false, List.of());
      }
      grouped.add(mapped);
    }
    return new MappingResult<>(true, List.copyOf(grouped));
  }

  private static MappingResult<Expression> mapHavingExpression(
      SQLiteParser.Select_coreContext core, ParseOptions options) {
    if (core.having_expr == null) {
      return new MappingResult<>(true, null);
    }
    Expression having = mapExpr(core.having_expr, options);
    if (having == null) {
      return new MappingResult<>(false, null);
    }
    return new MappingResult<>(true, having);
  }

  private static SqliteStatementKind kindForSqlStmt(SQLiteParser.Sql_stmtContext stmt) {
    SqliteStatementKind kind = kindForCreateStmt(stmt);
    if (kind != null) return kind;
    kind = kindForDmlStmt(stmt);
    if (kind != null) return kind;
    kind = kindForTransactionStmt(stmt);
    if (kind != null) return kind;
    return kindForMiscStmt(stmt);
  }

  private static SqliteStatementKind kindForCreateStmt(SQLiteParser.Sql_stmtContext stmt) {
    if (stmt.create_index_stmt() != null) return SqliteStatementKind.CREATE_INDEX;
    if (stmt.create_table_stmt() != null) return SqliteStatementKind.CREATE_TABLE;
    if (stmt.create_trigger_stmt() != null) return SqliteStatementKind.CREATE_TRIGGER;
    if (stmt.create_view_stmt() != null) return SqliteStatementKind.CREATE_VIEW;
    if (stmt.create_virtual_table_stmt() != null) return SqliteStatementKind.CREATE_VIRTUAL_TABLE;
    return null;
  }

  private static SqliteStatementKind kindForDmlStmt(SQLiteParser.Sql_stmtContext stmt) {
    if (stmt.delete_stmt() != null) return SqliteStatementKind.DELETE;
    if (stmt.insert_stmt() != null) return SqliteStatementKind.INSERT;
    if (stmt.update_stmt() != null) return SqliteStatementKind.UPDATE;
    return null;
  }

  private static SqliteStatementKind kindForTransactionStmt(SQLiteParser.Sql_stmtContext stmt) {
    if (stmt.begin_stmt() != null) return SqliteStatementKind.BEGIN;
    if (stmt.commit_stmt() != null) return SqliteStatementKind.COMMIT;
    if (stmt.rollback_stmt() != null) return SqliteStatementKind.ROLLBACK;
    if (stmt.savepoint_stmt() != null) return SqliteStatementKind.SAVEPOINT;
    if (stmt.release_stmt() != null) return SqliteStatementKind.RELEASE;
    return null;
  }

  private static SqliteStatementKind kindForMiscStmt(SQLiteParser.Sql_stmtContext stmt) {
    if (stmt.alter_table_stmt() != null) return SqliteStatementKind.ALTER_TABLE;
    if (stmt.analyze_stmt() != null) return SqliteStatementKind.ANALYZE;
    if (stmt.attach_stmt() != null) return SqliteStatementKind.ATTACH;
    if (stmt.detach_stmt() != null) return SqliteStatementKind.DETACH;
    if (stmt.drop_stmt() != null) return SqliteStatementKind.DROP;
    if (stmt.pragma_stmt() != null) return SqliteStatementKind.PRAGMA;
    if (stmt.reindex_stmt() != null) return SqliteStatementKind.REINDEX;
    if (stmt.vacuum_stmt() != null) return SqliteStatementKind.VACUUM;
    return SqliteStatementKind.OTHER;
  }

  private static String textOf(org.antlr.v4.runtime.ParserRuleContext context) {
    if (context.start == null || context.stop == null) {
      return "";
    }
    Interval interval = Interval.of(context.start.getStartIndex(), context.stop.getStopIndex());
    return context.start.getInputStream().getText(interval);
  }

  private record MappingResult<T>(boolean supported, T value) {}
}
