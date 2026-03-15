package io.github.e4c5.sqool.dialect.postgresql;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.BinaryExpression;
import io.github.e4c5.sqool.ast.BinaryOperator;
import io.github.e4c5.sqool.ast.ColumnAssignment;
import io.github.e4c5.sqool.ast.DeleteStatement;
import io.github.e4c5.sqool.ast.Expression;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.IdentifierExpression;
import io.github.e4c5.sqool.ast.InsertStatement;
import io.github.e4c5.sqool.ast.JoinTableReference;
import io.github.e4c5.sqool.ast.JoinType;
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
import io.github.e4c5.sqool.ast.UpdateStatement;
import io.github.e4c5.sqool.core.MappingResult;
import io.github.e4c5.sqool.core.ParseMetrics;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseResult;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SourceSpans;
import io.github.e4c5.sqool.core.SqlDialect;
import io.github.e4c5.sqool.grammar.postgresql.generated.PostgreSQLParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        SqlDialect.POSTGRESQL,
        new SqlScript(statements, scriptSpan),
        List.of(),
        ParseMetrics.unknown());
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
    if (stmt.insertStatement() != null) {
      return mapInsertStatement(stmt.insertStatement(), options);
    }
    if (stmt.updateStatement() != null) {
      return mapUpdateStatement(stmt.updateStatement(), options);
    }
    if (stmt.deleteStatement() != null) {
      return mapDeleteStatement(stmt.deleteStatement(), options);
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
        List.of(),
        ParseMetrics.unknown());
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
        String table = SourceSpans.textOf(tableAll.tableRef());
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
   * Maps a FROM clause to a {@link TableReference}. Supports single named tables and JOIN chains.
   * Returns null for derived tables or unsupported shapes to trigger a raw fallback.
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
    return mapTableReference(refs.get(0), options);
  }

  private static TableReference mapTableReference(
      PostgreSQLParser.TableReferenceContext ref, ParseOptions options) {
    PostgreSQLParser.TablePrimaryContext primary = ref.tablePrimary();
    TableReference current = mapTablePrimary(primary, options);
    if (current == null) {
      return null;
    }
    for (PostgreSQLParser.JoinClauseContext joinClause : ref.joinClause()) {
      current = mapJoinClause(current, joinClause, options);
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  private static TableReference mapTablePrimary(
      PostgreSQLParser.TablePrimaryContext primary, ParseOptions options) {
    if (!(primary instanceof PostgreSQLParser.NamedTableContext namedCtx)) {
      return null;
    }
    String tableName = namedCtx.qualifiedName().getText();
    String alias = namedCtx.alias != null ? namedCtx.alias.getText() : null;
    return new NamedTableReference(
        tableName, alias, SourceSpans.fromTokens(primary.start, primary.stop, options));
  }

  private static TableReference mapJoinClause(
      TableReference left, PostgreSQLParser.JoinClauseContext joinCtx, ParseOptions options) {
    if (joinCtx instanceof PostgreSQLParser.CrossJoinContext crossCtx) {
      TableReference right = mapTablePrimary(crossCtx.tablePrimary(), options);
      if (right == null) {
        return null;
      }
      return new JoinTableReference(
          left,
          JoinType.CROSS,
          right,
          null,
          List.of(),
          false,
          SourceSpans.fromTokens(joinCtx.start, joinCtx.stop, options));
    }
    if (joinCtx instanceof PostgreSQLParser.NaturalJoinContext naturalCtx) {
      JoinType joinType = mapJoinKind(naturalCtx.joinKind());
      TableReference right = mapTablePrimary(naturalCtx.tablePrimary(), options);
      if (right == null) {
        return null;
      }
      return new JoinTableReference(
          left,
          joinType,
          right,
          null,
          List.of(),
          true,
          SourceSpans.fromTokens(joinCtx.start, joinCtx.stop, options));
    }
    if (joinCtx instanceof PostgreSQLParser.QualifiedJoinContext qualCtx) {
      JoinType joinType = mapJoinKind(qualCtx.joinKind());
      TableReference right = mapTablePrimary(qualCtx.tablePrimary(), options);
      if (right == null) {
        return null;
      }
      Expression condition = null;
      List<String> usingColumns = List.of();
      if (qualCtx.expr() != null) {
        condition = mapExpr(qualCtx.expr(), options);
        if (condition == null) {
          return null;
        }
      } else if (qualCtx.columnList() != null) {
        usingColumns =
            qualCtx.columnList().columnName().stream()
                .map(PostgreSQLParser.ColumnNameContext::getText)
                .collect(Collectors.toList());
      }
      return new JoinTableReference(
          left,
          joinType,
          right,
          condition,
          usingColumns,
          false,
          SourceSpans.fromTokens(joinCtx.start, joinCtx.stop, options));
    }
    return null;
  }

  // =========================================================================
  // INSERT / UPDATE / DELETE
  // =========================================================================

  private static ParseResult mapInsertStatement(
      PostgreSQLParser.InsertStatementContext ctx, ParseOptions options) {
    if (ctx.onConflictClause() != null || ctx.returningClause() != null) {
      return rawStatement(ctx, PostgresqlStatementKind.INSERT, options);
    }

    String tableName = ctx.qualifiedName().getText();
    List<String> columns =
        ctx.columnList() != null
            ? ctx.columnList().columnName().stream()
                .map(PostgreSQLParser.ColumnNameContext::getText)
                .toList()
            : List.of();

    PostgreSQLParser.InsertSourceContext source = ctx.insertSource();
    if (source instanceof PostgreSQLParser.InsertValuesContext valuesCtx) {
      Optional<List<List<Expression>>> rowsOpt = mapInsertValues(valuesCtx, options);
      if (rowsOpt.isPresent()) {
        return new ParseSuccess(
            SqlDialect.POSTGRESQL,
            new InsertStatement(
                tableName,
                columns,
                rowsOpt.get(),
                List.of(),
                null,
                List.of(),
                false,
                SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
            List.of(),
            ParseMetrics.unknown());
      }
    } else if (source instanceof PostgreSQLParser.InsertSelectContext selectCtx) {
      Statement selectStmt = mapInsertSelect(selectCtx, options);
      if (selectStmt != null) {
        return new ParseSuccess(
            SqlDialect.POSTGRESQL,
            new InsertStatement(
                tableName,
                columns,
                List.of(),
                List.of(),
                selectStmt,
                List.of(),
                false,
                SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
            List.of(),
            ParseMetrics.unknown());
      }
    }

    return rawStatement(ctx, PostgresqlStatementKind.INSERT, options);
  }

  /** Returns parsed value rows, or empty if VALUES contain DEFAULT or unparseable expr. */
  private static Optional<List<List<Expression>>> mapInsertValues(
      PostgreSQLParser.InsertValuesContext valuesCtx, ParseOptions options) {
    List<List<Expression>> rows = new ArrayList<>();
    for (PostgreSQLParser.RowValuesContext rowCtx : valuesCtx.rowValues()) {
      List<Expression> row = new ArrayList<>();
      for (PostgreSQLParser.InsertExprContext exprCtx : rowCtx.insertExpr()) {
        if (exprCtx instanceof PostgreSQLParser.DefaultExprContext) {
          return Optional.empty();
        }
        Expression expr = mapExpr(((PostgreSQLParser.ValueExprContext) exprCtx).expr(), options);
        if (expr == null) {
          return Optional.empty();
        }
        row.add(expr);
      }
      rows.add(List.copyOf(row));
    }
    return Optional.of(rows);
  }

  /** Returns the SELECT statement for INSERT...SELECT, or null if not parseable. */
  private static Statement mapInsertSelect(
      PostgreSQLParser.InsertSelectContext selectCtx, ParseOptions options) {
    ParseResult selectResult = mapSelectStatement(selectCtx.selectStatement(), options);
    if (!(selectResult instanceof ParseSuccess success)) {
      return null;
    }
    return (Statement) success.root();
  }

  private static ParseResult mapUpdateStatement(
      PostgreSQLParser.UpdateStatementContext ctx, ParseOptions options) {
    if (ctx.ONLY() != null || ctx.fromClause() != null || ctx.returningClause() != null) {
      return rawStatement(ctx, PostgresqlStatementKind.UPDATE, options);
    }

    String tableName = ctx.qualifiedName().getText();
    String alias = ctx.alias != null ? ctx.alias.getText() : null;
    TableReference target =
        new NamedTableReference(
            tableName,
            alias,
            SourceSpans.fromTokens(ctx.qualifiedName().start, ctx.qualifiedName().stop, options));

    List<ColumnAssignment> assignments = new ArrayList<>();
    for (PostgreSQLParser.SetClauseContext setCtx : ctx.setClauseList().setClause()) {
      if (setCtx.DEFAULT_KW() != null) {
        return rawStatement(ctx, PostgresqlStatementKind.UPDATE, options);
      }
      Expression value = mapExpr(setCtx.expr(), options);
      if (value == null) {
        return rawStatement(ctx, PostgresqlStatementKind.UPDATE, options);
      }
      assignments.add(
          new ColumnAssignment(
              setCtx.columnName().getText(),
              value,
              SourceSpans.fromTokens(setCtx.start, setCtx.stop, options)));
    }

    MappingResult<Expression> whereResult = mapWhereClause(ctx.whereClause(), options);
    if (!whereResult.supported()) {
      return rawStatement(ctx, PostgresqlStatementKind.UPDATE, options);
    }

    return new ParseSuccess(
        SqlDialect.POSTGRESQL,
        new UpdateStatement(
            target,
            assignments,
            whereResult.value(),
            List.of(),
            null,
            false,
            SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
        List.of(),
        ParseMetrics.unknown());
  }

  private static ParseResult mapDeleteStatement(
      PostgreSQLParser.DeleteStatementContext ctx, ParseOptions options) {
    if (ctx.ONLY() != null
        || ctx.tableReference() != null && !ctx.tableReference().isEmpty()
        || ctx.returningClause() != null) {
      return rawStatement(ctx, PostgresqlStatementKind.DELETE, options);
    }

    String tableName = ctx.qualifiedName().getText();
    String alias = ctx.alias != null ? ctx.alias.getText() : null;
    TableReference target =
        new NamedTableReference(
            tableName,
            alias,
            SourceSpans.fromTokens(ctx.qualifiedName().start, ctx.qualifiedName().stop, options));

    MappingResult<Expression> whereResult = mapWhereClause(ctx.whereClause(), options);
    if (!whereResult.supported()) {
      return rawStatement(ctx, PostgresqlStatementKind.DELETE, options);
    }

    return new ParseSuccess(
        SqlDialect.POSTGRESQL,
        new DeleteStatement(
            target,
            whereResult.value(),
            List.of(),
            null,
            SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
        List.of(),
        ParseMetrics.unknown());
  }

  private static JoinType mapJoinKind(PostgreSQLParser.JoinKindContext ctx) {
    if (ctx == null) {
      return JoinType.INNER;
    }
    return JoinType.fromKind(ctx.LEFT() != null, ctx.RIGHT() != null, ctx.FULL() != null);
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
            kind, SourceSpans.textOf(ctx), SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
        List.of(),
        ParseMetrics.unknown());
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
}
