package io.github.e4c5.sqool.dialect.postgresql;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.BinaryExpression;
import io.github.e4c5.sqool.ast.BinaryOperator;
import io.github.e4c5.sqool.ast.Expression;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.IdentifierExpression;
import io.github.e4c5.sqool.ast.LimitClause;
import io.github.e4c5.sqool.ast.LiteralExpression;
import io.github.e4c5.sqool.ast.NamedTableReference;
import io.github.e4c5.sqool.ast.OrderByItem;
import io.github.e4c5.sqool.ast.PostgresqlRawStatement;
import io.github.e4c5.sqool.ast.PostgresqlStatementKind;
import io.github.e4c5.sqool.ast.SelectItem;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.SortDirection;
import io.github.e4c5.sqool.ast.SourceSpan;
import io.github.e4c5.sqool.ast.SqlScript;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.ast.TableReference;
import io.github.e4c5.sqool.ast.UnaryExpression;
import io.github.e4c5.sqool.ast.UnaryOperator;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SourceSpans;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.grammar.postgresql.generated.PostgreSQLParser;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.misc.Interval;

/** Maps the PostgreSQL ANTLR parse tree to the normalized sqool AST for the v1 subset. */
final class PostgresqlAstMapper {

  private PostgresqlAstMapper() {}

  // =========================================================================
  // Entry points
  // =========================================================================

  static ParseResult mapRoot(PostgreSQLParser.RootContext root, ParseOptions options) {
    List<Statement> statements = new ArrayList<>();
    List<PostgreSQLParser.StatementContext> stmts = root.statement();
    for (PostgreSQLParser.StatementContext stmt : stmts) {
      ParseResult one = mapStatement(stmt, options);
      if (one instanceof io.github.e4c5.sqool.core.ParseFailure) {
        return one;
      }
      statements.add((Statement) ((ParseSuccess) one).root());
    }
    SourceSpan scriptSpan =
        statements.isEmpty()
            ? null
            : SourceSpans.fromTokens(stmts.get(0).start, stmts.get(stmts.size() - 1).stop, options);
    return new ParseSuccess(
        SqlDialect.POSTGRESQL, new SqlScript(statements, scriptSpan), List.of());
  }

  static ParseResult mapSingleStatement(
      PostgreSQLParser.SingleStatementContext ctx, ParseOptions options) {
    return mapStatement(ctx.statement(), options);
  }

  // =========================================================================
  // Statement dispatch
  // =========================================================================

  static ParseResult mapStatement(PostgreSQLParser.StatementContext stmt, ParseOptions options) {
    if (stmt.selectStatement() != null) {
      return mapSelectStatement(stmt.selectStatement(), options);
    }
    PostgresqlStatementKind kind = kindForStatement(stmt);
    return rawStatement(stmt, kind, options);
  }

  private static ParseResult mapSelectStatement(
      PostgreSQLParser.SelectStatementContext ctx, ParseOptions options) {
    if (hasUnsupportedSelectShape(ctx) || ctx.offsetClause() != null || ctx.fetchClause() != null) {
      return rawStatement(ctx, PostgresqlStatementKind.SELECT, options);
    }

    PostgreSQLParser.QueryTermContext term = ctx.queryExpression().queryTerm(0);
    boolean distinct = term.DISTINCT() != null;

    MappingResult<List<SelectItem>> selectItemsResult = mapSelectList(term.selectList(), options);
    if (!selectItemsResult.supported()) {
      return rawStatement(ctx, PostgresqlStatementKind.SELECT, options);
    }

    TableReference from = null;
    if (term.fromClause() != null) {
      from = mapFromClause(term.fromClause(), options);
      if (from == null) {
        return rawStatement(ctx, PostgresqlStatementKind.SELECT, options);
      }
    }

    MappingResult<Expression> whereResult = mapWhereClause(term.whereClause(), options);
    if (!whereResult.supported()) {
      return rawStatement(ctx, PostgresqlStatementKind.SELECT, options);
    }

    MappingResult<List<Expression>> groupByResult = mapGroupByClause(term.groupByClause(), options);
    if (!groupByResult.supported()) {
      return rawStatement(ctx, PostgresqlStatementKind.SELECT, options);
    }

    MappingResult<Expression> havingResult = mapHavingClause(term.havingClause(), options);
    if (!havingResult.supported()) {
      return rawStatement(ctx, PostgresqlStatementKind.SELECT, options);
    }

    MappingResult<List<OrderByItem>> orderByResult = mapOrderByClause(ctx.orderByClause(), options);
    if (!orderByResult.supported()) {
      return rawStatement(ctx, PostgresqlStatementKind.SELECT, options);
    }

    LimitClause limit = null;
    if (ctx.limitClause() != null) {
      limit = mapLimitClause(ctx.limitClause(), options);
      if (limit == null) {
        return rawStatement(ctx, PostgresqlStatementKind.SELECT, options);
      }
    }

    return new ParseSuccess(
        SqlDialect.POSTGRESQL,
        new SelectStatement(
            distinct,
            selectItemsResult.value(),
            from,
            whereResult.value(),
            groupByResult.value(),
            havingResult.value(),
            orderByResult.value(),
            limit,
            SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
        List.of());
  }

  // =========================================================================
  // SELECT list
  // =========================================================================

  private static MappingResult<List<SelectItem>> mapSelectList(
      PostgreSQLParser.SelectListContext ctx, ParseOptions options) {
    if (ctx == null) {
      return new MappingResult<>(false, List.of());
    }
    List<SelectItem> items = new ArrayList<>();
    for (PostgreSQLParser.SelectItemContext item : ctx.selectItem()) {
      if (item instanceof PostgreSQLParser.AllColumnsItemContext) {
        items.add(
            new AllColumnsSelectItem(null, SourceSpans.fromTokens(item.start, item.stop, options)));
      } else if (item instanceof PostgreSQLParser.TableAllColumnsItemContext tableAll) {
        String table = textOf(tableAll.tableRef());
        items.add(
            new AllColumnsSelectItem(
                table, SourceSpans.fromTokens(item.start, item.stop, options)));
      } else if (item instanceof PostgreSQLParser.ExpressionItemContext exprItem) {
        Expression expr = mapExpr(exprItem.expr(), options);
        if (expr == null) {
          return new MappingResult<>(false, List.of());
        }
        String alias = exprItem.alias != null ? exprItem.alias.getText() : null;
        items.add(
            new ExpressionSelectItem(
                expr, alias, SourceSpans.fromTokens(item.start, item.stop, options)));
      } else {
        return new MappingResult<>(false, List.of());
      }
    }
    return new MappingResult<>(true, List.copyOf(items));
  }

  // =========================================================================
  // FROM clause
  // =========================================================================

  /**
   * Maps a simple FROM clause (single named table, no joins) to a {@link NamedTableReference}.
   * Returns null for joins or derived tables to trigger a raw fallback.
   */
  private static TableReference mapFromClause(
      PostgreSQLParser.FromClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return null;
    }
    List<PostgreSQLParser.TableReferenceContext> refs = ctx.tableReference();
    if (refs.size() != 1) {
      return null;
    }
    PostgreSQLParser.TableReferenceContext ref = refs.get(0);
    if (!ref.joinClause().isEmpty()) {
      return null;
    }
    PostgreSQLParser.TablePrimaryContext primary = ref.tablePrimary();
    if (!(primary instanceof PostgreSQLParser.NamedTableContext namedCtx)) {
      return null;
    }
    String tableName = namedCtx.qualifiedName().getText();
    String alias = namedCtx.alias != null ? namedCtx.alias.getText() : null;
    return new NamedTableReference(
        tableName, alias, SourceSpans.fromTokens(primary.start, primary.stop, options));
  }

  // =========================================================================
  // WHERE / GROUP BY / HAVING
  // =========================================================================

  private static MappingResult<Expression> mapWhereClause(
      PostgreSQLParser.WhereClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return new MappingResult<>(true, null);
    }
    Expression expr = mapExpr(ctx.expr(), options);
    if (expr == null) {
      return new MappingResult<>(false, null);
    }
    return new MappingResult<>(true, expr);
  }

  private static MappingResult<List<Expression>> mapGroupByClause(
      PostgreSQLParser.GroupByClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return new MappingResult<>(true, List.of());
    }
    List<Expression> items = new ArrayList<>();
    for (PostgreSQLParser.ExprContext exprCtx : ctx.expr()) {
      Expression expr = mapExpr(exprCtx, options);
      if (expr == null) {
        return new MappingResult<>(false, List.of());
      }
      items.add(expr);
    }
    return new MappingResult<>(true, List.copyOf(items));
  }

  private static MappingResult<Expression> mapHavingClause(
      PostgreSQLParser.HavingClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return new MappingResult<>(true, null);
    }
    Expression expr = mapExpr(ctx.expr(), options);
    if (expr == null) {
      return new MappingResult<>(false, null);
    }
    return new MappingResult<>(true, expr);
  }

  // =========================================================================
  // ORDER BY / LIMIT
  // =========================================================================

  private static MappingResult<List<OrderByItem>> mapOrderByClause(
      PostgreSQLParser.OrderByClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return new MappingResult<>(true, List.of());
    }
    List<OrderByItem> items = new ArrayList<>();
    for (PostgreSQLParser.OrderByItemContext item : ctx.orderByItem()) {
      if (item.NULLS() != null) {
        return new MappingResult<>(false, List.of());
      }
      Expression expr = mapExpr(item.expr(), options);
      if (expr == null) {
        return new MappingResult<>(false, List.of());
      }
      SortDirection dir = SortDirection.ASC;
      if (item.direction != null && item.direction.getType() == PostgreSQLParser.DESC) {
        dir = SortDirection.DESC;
      }
      items.add(new OrderByItem(expr, dir, SourceSpans.fromTokens(item.start, item.stop, options)));
    }
    return new MappingResult<>(true, List.copyOf(items));
  }

  private static LimitClause mapLimitClause(
      PostgreSQLParser.LimitClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return null;
    }
    // LIMIT ALL is not representable in the current AST; fall back.
    if (ctx.ALL() != null) {
      return null;
    }
    Long rowCount = parseLongLiteral(ctx.expr());
    if (rowCount == null) {
      return null;
    }
    return new LimitClause(rowCount, null, SourceSpans.fromTokens(ctx.start, ctx.stop, options));
  }

  // =========================================================================
  // Expression mapping
  // =========================================================================

  /**
   * Maps an expression context to an AST {@link Expression}. Returns null for expression shapes
   * that are outside the v1 subset; the caller should then fall back to a raw statement.
   */
  private static Expression mapExpr(PostgreSQLParser.ExprContext ctx, ParseOptions options) {
    if (ctx == null) {
      return null;
    }
    if (ctx instanceof PostgreSQLParser.LiteralExprContext literalCtx) {
      return mapLiteral(literalCtx.literal(), options);
    }
    if (ctx instanceof PostgreSQLParser.NameExprContext nameCtx) {
      return new IdentifierExpression(
          nameCtx.qualifiedName().getText(), SourceSpans.fromTokens(ctx.start, ctx.stop, options));
    }
    if (ctx instanceof PostgreSQLParser.ParenExprContext parenCtx) {
      return mapExpr(parenCtx.expr(), options);
    }
    if (ctx instanceof PostgreSQLParser.UnaryExprContext unaryCtx) {
      return mapUnaryExpr(unaryCtx, options);
    }
    if (ctx instanceof PostgreSQLParser.NotExprContext notCtx) {
      return mapNotExpr(notCtx, options);
    }
    if (ctx instanceof PostgreSQLParser.AndExprContext andCtx) {
      return mapBinaryExpr(andCtx.expr(0), andCtx.expr(1), BinaryOperator.AND, options);
    }
    if (ctx instanceof PostgreSQLParser.OrExprContext orCtx) {
      return mapBinaryExpr(orCtx.expr(0), orCtx.expr(1), BinaryOperator.OR, options);
    }
    if (ctx instanceof PostgreSQLParser.AddExprContext addCtx) {
      return mapAddExpr(addCtx, options);
    }
    if (ctx instanceof PostgreSQLParser.MulExprContext mulCtx) {
      return mapMulExpr(mulCtx, options);
    }
    if (ctx instanceof PostgreSQLParser.CompExprContext compCtx) {
      return mapCompExpr(compCtx, options);
    }
    if (ctx instanceof PostgreSQLParser.IsNullExprContext isNullCtx) {
      return mapIsNullExpr(isNullCtx, options);
    }
    // For any other expression shape, signal unsupported so caller can fall back to raw.
    return null;
  }

  private static Expression mapUnaryExpr(
      PostgreSQLParser.UnaryExprContext ctx, ParseOptions options) {
    int opType = ctx.op.getType();
    UnaryOperator op;
    if (opType == PostgreSQLParser.MINUS) {
      op = UnaryOperator.MINUS;
    } else if (opType == PostgreSQLParser.PLUS) {
      op = UnaryOperator.PLUS;
    } else {
      return null;
    }
    Expression operand = mapExpr(ctx.expr(), options);
    if (operand == null) {
      return null;
    }
    return new UnaryExpression(op, operand, SourceSpans.fromTokens(ctx.start, ctx.stop, options));
  }

  private static Expression mapNotExpr(PostgreSQLParser.NotExprContext ctx, ParseOptions options) {
    Expression operand = mapExpr(ctx.expr(), options);
    if (operand == null) {
      return null;
    }
    return new UnaryExpression(
        UnaryOperator.NOT, operand, SourceSpans.fromTokens(ctx.start, ctx.stop, options));
  }

  private static Expression mapAddExpr(PostgreSQLParser.AddExprContext ctx, ParseOptions options) {
    BinaryOperator op =
        ctx.op.getType() == PostgreSQLParser.PLUS ? BinaryOperator.PLUS : BinaryOperator.MINUS;
    return mapBinaryExpr(ctx.expr(0), ctx.expr(1), op, options);
  }

  private static Expression mapMulExpr(PostgreSQLParser.MulExprContext ctx, ParseOptions options) {
    int opType = ctx.op.getType();
    BinaryOperator op;
    if (opType == PostgreSQLParser.STAR) {
      op = BinaryOperator.MULTIPLY;
    } else if (opType == PostgreSQLParser.SLASH) {
      op = BinaryOperator.DIVIDE;
    } else {
      op = BinaryOperator.MODULO;
    }
    return mapBinaryExpr(ctx.expr(0), ctx.expr(1), op, options);
  }

  private static Expression mapCompExpr(
      PostgreSQLParser.CompExprContext ctx, ParseOptions options) {
    BinaryOperator op = mapCompOp(ctx.op.getType());
    if (op == null) {
      return null;
    }
    return mapBinaryExpr(ctx.expr(0), ctx.expr(1), op, options);
  }

  private static Expression mapIsNullExpr(
      PostgreSQLParser.IsNullExprContext ctx, ParseOptions options) {
    Expression operand = mapExpr(ctx.expr(), options);
    if (operand == null) {
      return null;
    }
    BinaryOperator op = ctx.NOT() != null ? BinaryOperator.IS_NOT : BinaryOperator.IS;
    return new BinaryExpression(
        operand,
        op,
        new LiteralExpression("NULL", null),
        SourceSpans.fromTokens(ctx.start, ctx.stop, options));
  }

  private static Expression mapBinaryExpr(
      PostgreSQLParser.ExprContext left,
      PostgreSQLParser.ExprContext right,
      BinaryOperator op,
      ParseOptions options) {
    Expression leftExpr = mapExpr(left, options);
    Expression rightExpr = mapExpr(right, options);
    if (leftExpr == null || rightExpr == null) {
      return null;
    }
    return new BinaryExpression(
        leftExpr, op, rightExpr, SourceSpans.fromTokens(left.start, right.stop, options));
  }

  private static BinaryOperator mapCompOp(int tokenType) {
    return switch (tokenType) {
      case PostgreSQLParser.EQ -> BinaryOperator.EQUAL;
      case PostgreSQLParser.NEQ -> BinaryOperator.NOT_EQUAL;
      case PostgreSQLParser.LT -> BinaryOperator.LESS_THAN;
      case PostgreSQLParser.LTE -> BinaryOperator.LESS_OR_EQUAL;
      case PostgreSQLParser.GT -> BinaryOperator.GREATER_THAN;
      case PostgreSQLParser.GTE -> BinaryOperator.GREATER_OR_EQUAL;
      default -> null;
    };
  }

  private static Expression mapLiteral(PostgreSQLParser.LiteralContext ctx, ParseOptions options) {
    if (ctx == null) {
      return null;
    }
    return new LiteralExpression(
        ctx.getText(), SourceSpans.fromTokens(ctx.start, ctx.stop, options));
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private static boolean hasUnsupportedSelectShape(PostgreSQLParser.SelectStatementContext ctx) {
    // CTEs not supported in normalized AST
    if (ctx.withClause() != null) {
      return true;
    }
    // SET operations (UNION, INTERSECT, EXCEPT) not supported
    PostgreSQLParser.QueryExpressionContext qe = ctx.queryExpression();
    if (!qe.setOperator().isEmpty()) {
      return true;
    }
    // Parenthesized sub-select at the top level not supported
    PostgreSQLParser.QueryTermContext term = qe.queryTerm(0);
    return term.SELECT() == null;
  }

  private static ParseResult rawStatement(
      org.antlr.v4.runtime.ParserRuleContext ctx,
      PostgresqlStatementKind kind,
      ParseOptions options) {
    return new ParseSuccess(
        SqlDialect.POSTGRESQL,
        new PostgresqlRawStatement(
            kind, textOf(ctx), SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
        List.of());
  }

  private static PostgresqlStatementKind kindForStatement(PostgreSQLParser.StatementContext stmt) {
    if (stmt.selectStatement() != null) return PostgresqlStatementKind.SELECT;
    if (stmt.insertStatement() != null) return PostgresqlStatementKind.INSERT;
    if (stmt.updateStatement() != null) return PostgresqlStatementKind.UPDATE;
    if (stmt.deleteStatement() != null) return PostgresqlStatementKind.DELETE;
    if (stmt.createTableStatement() != null) return PostgresqlStatementKind.CREATE_TABLE;
    if (stmt.dropTableStatement() != null) return PostgresqlStatementKind.DROP_TABLE;
    if (stmt.truncateStatement() != null) return PostgresqlStatementKind.TRUNCATE;
    if (stmt.beginStatement() != null) return PostgresqlStatementKind.BEGIN;
    if (stmt.commitStatement() != null) return PostgresqlStatementKind.COMMIT;
    if (stmt.rollbackStatement() != null) return PostgresqlStatementKind.ROLLBACK;
    return PostgresqlStatementKind.OTHER;
  }

  private static Long parseLongLiteral(PostgreSQLParser.ExprContext ctx) {
    if (ctx instanceof PostgreSQLParser.LiteralExprContext litCtx
        && litCtx.literal() instanceof PostgreSQLParser.IntLiteralContext) {
      try {
        return Long.parseLong(litCtx.literal().getText());
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }

  private static String textOf(org.antlr.v4.runtime.ParserRuleContext ctx) {
    if (ctx == null || ctx.start == null || ctx.stop == null) {
      return "";
    }
    Interval interval = Interval.of(ctx.start.getStartIndex(), ctx.stop.getStopIndex());
    return ctx.start.getInputStream().getText(interval);
  }

  private record MappingResult<T>(boolean supported, T value) {}
}
