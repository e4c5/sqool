package io.github.e4c5.sqool.dialect.sqlite;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.Expression;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
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
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SourceSpans;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.grammar.sqlite.generated.SQLiteParser;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.misc.Interval;

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
    return new ParseSuccess(SqlDialect.SQLITE, new SqlScript(statements, scriptSpan), List.of());
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
        List.of());
  }

  private static ParseResult mapSelectStmt(
      SQLiteParser.Select_stmtContext context, ParseOptions options) {
    if (context.with_clause() != null) {
      return rawSelect(context, options);
    }
    if (context.compound_operator() != null && !context.compound_operator().isEmpty()) {
      return rawSelect(context, options);
    }
    SQLiteParser.Select_coreContext core = context.select_core(0);
    if (core.values_clause() != null) {
      return rawSelect(context, options);
    }
    if (core.WINDOW_() != null) {
      return rawSelect(context, options);
    }

    boolean distinct = core.DISTINCT_() != null;
    List<SelectItem> selectItems = mapResultColumns(core.result_column(), options);
    if (selectItems == null) {
      return rawSelect(context, options);
    }

    TableReference from = core.FROM_() != null ? mapJoinClause(core.join_clause(), options) : null;
    if (core.FROM_() != null && from == null) {
      // FROM clause present but not representable in the current AST slice.
      return rawSelect(context, options);
    }

    Expression where = null;
    if (core.where_expr != null) {
      where = mapExpr(core.where_expr, options);
      if (where == null) {
        return rawSelect(context, options);
      }
    }

    List<Expression> groupBy = List.of();
    if (core.group_by_expr != null && !core.group_by_expr.isEmpty()) {
      var grouped = new ArrayList<Expression>();
      for (SQLiteParser.ExprContext groupExpr : core.group_by_expr) {
        Expression mapped = mapExpr(groupExpr, options);
        if (mapped == null) {
          return rawSelect(context, options);
        }
        grouped.add(mapped);
      }
      groupBy = List.copyOf(grouped);
    }

    Expression having = null;
    if (core.having_expr != null) {
      having = mapExpr(core.having_expr, options);
      if (having == null) {
        return rawSelect(context, options);
      }
    }

    List<OrderByItem> orderBy = List.of();
    if (context.order_clause() != null) {
      orderBy = mapOrderClause(context.order_clause(), options);
      if (orderBy == null) {
        return rawSelect(context, options);
      }
    }

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
            selectItems,
            from,
            where,
            groupBy,
            having,
            orderBy,
            limit,
            SourceSpans.fromTokens(context.start, context.stop, options)),
        List.of());
  }

  private static ParseResult rawSelect(
      SQLiteParser.Select_stmtContext context, ParseOptions options) {
    return new ParseSuccess(
        SqlDialect.SQLITE,
        new SqliteRawStatement(
            SqliteStatementKind.SELECT,
            textOf(context),
            SourceSpans.fromTokens(context.start, context.stop, options)),
        List.of());
  }

  private static List<SelectItem> mapResultColumns(
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
          return null;
        }
        String alias = col.column_alias() != null ? col.column_alias().getText() : null;
        items.add(
            new ExpressionSelectItem(
                expr, alias, SourceSpans.fromTokens(col.start, col.stop, options)));
      }
    }
    return items;
  }

  private static TableReference mapJoinClause(
      SQLiteParser.Join_clauseContext context, ParseOptions options) {
    if (context == null) {
      return null;
    }
    // Only support a single table with no explicit join operators or constraints. Anything more
    // complex (joins, multiple tables) is treated as unsupported so the caller can fall back to
    // a raw statement.
    if (context.join_operator() != null && !context.join_operator().isEmpty()) {
      return null;
    }
    if (context.join_constraint() != null && !context.join_constraint().isEmpty()) {
      return null;
    }

    List<SQLiteParser.Table_or_subqueryContext> tables = context.table_or_subquery();
    if (tables.size() != 1) {
      return null;
    }
    SQLiteParser.Table_or_subqueryContext first = tables.get(0);
    if (first.select_stmt() != null) {
      return null;
    }
    if (first.table_function_name() != null) {
      return null;
    }
    if (first.OPEN_PAR() != null) {
      return null;
    }
    String tableName = first.table_name() != null ? first.table_name().getText() : null;
    if (tableName == null) {
      return null;
    }
    String alias = first.table_alias() != null ? first.table_alias().getText() : null;
    return new NamedTableReference(
        tableName, alias, SourceSpans.fromTokens(first.start, first.stop, options));
  }

  private static Expression mapExpr(SQLiteParser.ExprContext context, ParseOptions options) {
    SQLiteParser.Expr_orContext or = context.expr_or();
    if (or == null || or.expr_and().isEmpty()) {
      return null;
    }

    // Build a left-associative OR chain of AND expressions.
    Expression current = mapAnd(or.expr_and(0), options);
    if (current == null) {
      return null;
    }

    for (int i = 1; i < or.expr_and().size(); i++) {
      Expression rhs = mapAnd(or.expr_and(i), options);
      if (rhs == null) {
        return null;
      }
      current =
          new io.github.e4c5.sqool.ast.BinaryExpression(
              current,
              io.github.e4c5.sqool.ast.BinaryOperator.OR,
              rhs,
              SourceSpans.fromTokens(context.start, context.stop, options));
    }

    return current;
  }

  private static Expression mapAnd(
      SQLiteParser.Expr_andContext context, ParseOptions options) {
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

  private static Expression mapNot(
      SQLiteParser.Expr_notContext context, ParseOptions options) {
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
    if (context == null || context.expr_comparison().isEmpty()) {
      return null;
    }
    // Only the simple comparison chain is supported; anything more complex causes a fallback.
    if (context.expr_comparison().size() != 1) {
      return null;
    }
    SQLiteParser.Expr_comparisonContext comp = context.expr_comparison(0);
    if (comp.expr_bitwise().isEmpty() || comp.expr_bitwise().size() != 1) {
      return null;
    }
    SQLiteParser.Expr_bitwiseContext bit = comp.expr_bitwise(0);
    if (bit.expr_addition().isEmpty() || bit.expr_addition().size() != 1) {
      return null;
    }
    SQLiteParser.Expr_additionContext add = bit.expr_addition(0);
    if (add.expr_multiplication().isEmpty() || add.expr_multiplication().size() != 1) {
      return null;
    }
    SQLiteParser.Expr_multiplicationContext mul = add.expr_multiplication(0);
    if (mul.expr_string().isEmpty() || mul.expr_string().size() != 1) {
      return null;
    }
    SQLiteParser.Expr_stringContext str = mul.expr_string(0);
    if (str.expr_collate().isEmpty() || str.expr_collate().size() != 1) {
      return null;
    }
    SQLiteParser.Expr_collateContext coll = str.expr_collate(0);
    SQLiteParser.Expr_unaryContext un = coll.expr_unary();
    if (un == null) {
      return null;
    }
    SQLiteParser.Expr_baseContext base = un.expr_base();
    if (base == null) {
      return null;
    }
    if (base.literal_value() != null) {
      return mapLiteralValue(base.literal_value(), options);
    }
    if (base.BIND_PARAMETER() != null) {
      return new LiteralExpression(
          base.getText(), SourceSpans.fromTokens(base.start, base.stop, options));
    }
    if (base.column_name_excluding_string() != null) {
      return new io.github.e4c5.sqool.ast.IdentifierExpression(
          base.column_name_excluding_string().getText(),
          SourceSpans.fromTokens(base.start, base.stop, options));
    }
    if (base.table_name() != null && base.DOT() != null && base.column_name() != null) {
      String table = base.table_name().getText();
      String column = base.column_name().getText();
      return new io.github.e4c5.sqool.ast.IdentifierExpression(
          table + "." + column, SourceSpans.fromTokens(base.start, base.stop, options));
    }
    return new LiteralExpression(
        base.getText(), SourceSpans.fromTokens(base.start, base.stop, options));
  }

  private static Expression mapLiteralValue(
      SQLiteParser.Literal_valueContext context, ParseOptions options) {
    return new LiteralExpression(
        context.getText(), SourceSpans.fromTokens(context.start, context.stop, options));
  }

  private static List<OrderByItem> mapOrderClause(
      SQLiteParser.Order_clauseContext context, ParseOptions options) {
    List<OrderByItem> items = new ArrayList<>();
    for (SQLiteParser.Ordering_termContext term : context.ordering_term()) {
      Expression expr = mapExpr(term.expr(), options);
      if (expr == null) {
        return null;
      }
      SortDirection dir = SortDirection.ASC;
      if (term.asc_desc() != null && term.asc_desc().DESC_() != null) {
        dir = SortDirection.DESC;
      }
      items.add(new OrderByItem(expr, dir, SourceSpans.fromTokens(term.start, term.stop, options)));
    }
    return items;
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

  private static SqliteStatementKind kindForSqlStmt(SQLiteParser.Sql_stmtContext stmt) {
    if (stmt.alter_table_stmt() != null) return SqliteStatementKind.ALTER_TABLE;
    if (stmt.analyze_stmt() != null) return SqliteStatementKind.ANALYZE;
    if (stmt.attach_stmt() != null) return SqliteStatementKind.ATTACH;
    if (stmt.begin_stmt() != null) return SqliteStatementKind.BEGIN;
    if (stmt.commit_stmt() != null) return SqliteStatementKind.COMMIT;
    if (stmt.create_index_stmt() != null) return SqliteStatementKind.CREATE_INDEX;
    if (stmt.create_table_stmt() != null) return SqliteStatementKind.CREATE_TABLE;
    if (stmt.create_trigger_stmt() != null) return SqliteStatementKind.CREATE_TRIGGER;
    if (stmt.create_view_stmt() != null) return SqliteStatementKind.CREATE_VIEW;
    if (stmt.create_virtual_table_stmt() != null) return SqliteStatementKind.CREATE_VIRTUAL_TABLE;
    if (stmt.delete_stmt() != null) return SqliteStatementKind.DELETE;
    if (stmt.detach_stmt() != null) return SqliteStatementKind.DETACH;
    if (stmt.drop_stmt() != null) return SqliteStatementKind.DROP;
    if (stmt.insert_stmt() != null) return SqliteStatementKind.INSERT;
    if (stmt.pragma_stmt() != null) return SqliteStatementKind.PRAGMA;
    if (stmt.reindex_stmt() != null) return SqliteStatementKind.REINDEX;
    if (stmt.release_stmt() != null) return SqliteStatementKind.RELEASE;
    if (stmt.rollback_stmt() != null) return SqliteStatementKind.ROLLBACK;
    if (stmt.savepoint_stmt() != null) return SqliteStatementKind.SAVEPOINT;
    if (stmt.update_stmt() != null) return SqliteStatementKind.UPDATE;
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
}
