package io.github.e4c5.sqool.dialect.oracle;

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
import io.github.e4c5.sqool.ast.LiteralExpression;
import io.github.e4c5.sqool.ast.NamedTableReference;
import io.github.e4c5.sqool.ast.OracleRawStatement;
import io.github.e4c5.sqool.ast.OracleStatementKind;
import io.github.e4c5.sqool.ast.OrderByItem;
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
import io.github.e4c5.sqool.grammar.oracle.generated.OracleParser;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Maps the Oracle ANTLR parse tree to the normalized sqool AST for the v1 subset. */
final class OracleAstMapper {

  private OracleAstMapper() {}

  // =========================================================================
  // Entry points
  // =========================================================================

  static ParseResult mapRoot(OracleParser.RootContext root, ParseOptions options) {
    List<Statement> statements = new ArrayList<>();
    List<OracleParser.StatementContext> stmts = root.statement();
    for (OracleParser.StatementContext stmt : stmts) {
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
        SqlDialect.ORACLE,
        new SqlScript(statements, scriptSpan),
        List.of(),
        ParseMetrics.unknown());
  }

  static ParseResult mapSingleStatement(
      OracleParser.SingleStatementContext ctx, ParseOptions options) {
    return mapStatement(ctx.statement(), options);
  }

  // =========================================================================
  // Statement dispatch
  // =========================================================================

  static ParseResult mapStatement(OracleParser.StatementContext stmt, ParseOptions options) {
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
    OracleStatementKind kind = kindForStatement(stmt);
    return rawStatement(stmt, kind, options);
  }

  private static ParseResult mapSelectStatement(
      OracleParser.SelectStatementContext ctx, ParseOptions options) {
    if (hasUnsupportedSelectShape(ctx)) {
      return rawStatement(ctx, OracleStatementKind.SELECT, options);
    }

    OracleParser.QueryTermContext term = ctx.queryExpression().queryTerm(0);
    boolean distinct = term.DISTINCT() != null;

    MappingResult<List<SelectItem>> selectItemsResult = mapSelectList(term.selectList(), options);
    if (!selectItemsResult.supported()) {
      return rawStatement(ctx, OracleStatementKind.SELECT, options);
    }

    TableReference from = null;
    if (term.fromClause() != null) {
      from = mapFromClause(term.fromClause(), options);
      if (from == null) {
        return rawStatement(ctx, OracleStatementKind.SELECT, options);
      }
    }

    MappingResult<Expression> whereResult = mapWhereClause(term.whereClause(), options);
    if (!whereResult.supported()) {
      return rawStatement(ctx, OracleStatementKind.SELECT, options);
    }

    MappingResult<List<Expression>> groupByResult = mapGroupByClause(term.groupByClause(), options);
    if (!groupByResult.supported()) {
      return rawStatement(ctx, OracleStatementKind.SELECT, options);
    }

    MappingResult<Expression> havingResult = mapHavingClause(term.havingClause(), options);
    if (!havingResult.supported()) {
      return rawStatement(ctx, OracleStatementKind.SELECT, options);
    }

    MappingResult<List<OrderByItem>> orderByResult = mapOrderByClause(ctx.orderByClause(), options);
    if (!orderByResult.supported()) {
      return rawStatement(ctx, OracleStatementKind.SELECT, options);
    }

    return new ParseSuccess(
        SqlDialect.ORACLE,
        new SelectStatement(
            distinct,
            selectItemsResult.value(),
            from,
            whereResult.value(),
            groupByResult.value(),
            havingResult.value(),
            orderByResult.value(),
            null,
            SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
        List.of(),
        ParseMetrics.unknown());
  }

  // =========================================================================
  // SELECT list
  // =========================================================================

  private static MappingResult<List<SelectItem>> mapSelectList(
      OracleParser.SelectListContext ctx, ParseOptions options) {
    if (ctx == null) {
      return new MappingResult<>(false, List.of());
    }
    List<SelectItem> items = new ArrayList<>();
    for (OracleParser.SelectItemContext item : ctx.selectItem()) {
      if (item instanceof OracleParser.AllColumnsItemContext) {
        items.add(
            new AllColumnsSelectItem(null, SourceSpans.fromTokens(item.start, item.stop, options)));
      } else if (item instanceof OracleParser.TableAllColumnsItemContext tableAll) {
        String table = SourceSpans.textOf(tableAll.tableRef());
        items.add(
            new AllColumnsSelectItem(
                table, SourceSpans.fromTokens(item.start, item.stop, options)));
      } else if (item instanceof OracleParser.ExpressionItemContext exprItem) {
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
      OracleParser.FromClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return null;
    }
    List<OracleParser.TableReferenceContext> refs = ctx.tableReference();
    if (refs.size() != 1) {
      return null;
    }
    return mapTableReference(refs.get(0), options);
  }

  private static TableReference mapTableReference(
      OracleParser.TableReferenceContext ref, ParseOptions options) {
    OracleParser.TablePrimaryContext primary = ref.tablePrimary();
    TableReference current = mapTablePrimary(primary, options);
    if (current == null) {
      return null;
    }
    for (OracleParser.JoinClauseContext joinClause : ref.joinClause()) {
      current = mapJoinClause(current, joinClause, options);
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  private static TableReference mapTablePrimary(
      OracleParser.TablePrimaryContext primary, ParseOptions options) {
    if (!(primary instanceof OracleParser.NamedTableContext namedCtx)) {
      return null;
    }
    String tableName = namedCtx.qualifiedName().getText();
    String alias = namedCtx.alias != null ? namedCtx.alias.getText() : null;
    return new NamedTableReference(
        tableName, alias, SourceSpans.fromTokens(primary.start, primary.stop, options));
  }

  private static TableReference mapJoinClause(
      TableReference left, OracleParser.JoinClauseContext joinCtx, ParseOptions options) {
    if (joinCtx instanceof OracleParser.CrossJoinContext crossCtx) {
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
    if (joinCtx instanceof OracleParser.NaturalJoinContext naturalCtx) {
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
    if (joinCtx instanceof OracleParser.QualifiedJoinContext qualCtx) {
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
                .map(OracleParser.ColumnNameContext::getText)
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
      OracleParser.InsertStatementContext ctx, ParseOptions options) {
    String tableName = ctx.qualifiedName().getText();
    List<String> columns = List.of();
    if (ctx.columnList() != null) {
      columns =
          ctx.columnList().columnName().stream()
              .map(OracleParser.ColumnNameContext::getText)
              .toList();
    }

    OracleParser.InsertSourceContext source = ctx.insertSource();
    if (source instanceof OracleParser.InsertValuesContext valuesCtx) {
      List<List<Expression>> rows = new ArrayList<>();
      for (OracleParser.RowValuesContext rowCtx : valuesCtx.rowValues()) {
        List<Expression> row = new ArrayList<>();
        for (OracleParser.InsertExprContext exprCtx : rowCtx.insertExpr()) {
          if (exprCtx instanceof OracleParser.DefaultExprContext) {
            return rawStatement(ctx, OracleStatementKind.INSERT, options);
          }
          Expression expr = mapExpr(((OracleParser.ValueExprContext) exprCtx).expr(), options);
          if (expr == null) {
            return rawStatement(ctx, OracleStatementKind.INSERT, options);
          }
          row.add(expr);
        }
        rows.add(List.copyOf(row));
      }
      return new ParseSuccess(
          SqlDialect.ORACLE,
          new InsertStatement(
              tableName,
              columns,
              rows,
              List.of(),
              null,
              List.of(),
              false,
              SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
          List.of(),
          ParseMetrics.unknown());
    } else if (source instanceof OracleParser.InsertSelectContext selectCtx) {
      ParseResult selectResult = mapSelectStatement(selectCtx.selectStatement(), options);
      if (!(selectResult instanceof ParseSuccess success)) {
        return rawStatement(ctx, OracleStatementKind.INSERT, options);
      }
      // Only build a normalized INSERT when the source mapped to a fully-normalized
      // SelectStatement; a raw-fallback root indicates an unsupported SELECT shape.
      if (!(success.root() instanceof SelectStatement)) {
        return rawStatement(ctx, OracleStatementKind.INSERT, options);
      }
      return new ParseSuccess(
          SqlDialect.ORACLE,
          new InsertStatement(
              tableName,
              columns,
              List.of(),
              List.of(),
              (Statement) success.root(),
              List.of(),
              false,
              SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
          List.of(),
          ParseMetrics.unknown());
    }

    return rawStatement(ctx, OracleStatementKind.INSERT, options);
  }

  private static ParseResult mapUpdateStatement(
      OracleParser.UpdateStatementContext ctx, ParseOptions options) {
    String tableName = ctx.qualifiedName().getText();
    String alias = ctx.alias != null ? ctx.alias.getText() : null;
    TableReference target =
        new NamedTableReference(
            tableName,
            alias,
            SourceSpans.fromTokens(ctx.qualifiedName().start, ctx.qualifiedName().stop, options));

    List<ColumnAssignment> assignments = new ArrayList<>();
    for (OracleParser.SetClauseContext setCtx : ctx.setClauseList().setClause()) {
      if (setCtx.DEFAULT_KW() != null) {
        return rawStatement(ctx, OracleStatementKind.UPDATE, options);
      }
      Expression value = mapExpr(setCtx.expr(), options);
      if (value == null) {
        return rawStatement(ctx, OracleStatementKind.UPDATE, options);
      }
      assignments.add(
          new ColumnAssignment(
              setCtx.columnName().getText(),
              value,
              SourceSpans.fromTokens(setCtx.start, setCtx.stop, options)));
    }

    MappingResult<Expression> whereResult = mapWhereClause(ctx.whereClause(), options);
    if (!whereResult.supported()) {
      return rawStatement(ctx, OracleStatementKind.UPDATE, options);
    }

    return new ParseSuccess(
        SqlDialect.ORACLE,
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
      OracleParser.DeleteStatementContext ctx, ParseOptions options) {
    String tableName = ctx.qualifiedName().getText();
    String alias = ctx.alias != null ? ctx.alias.getText() : null;
    TableReference target =
        new NamedTableReference(
            tableName,
            alias,
            SourceSpans.fromTokens(ctx.qualifiedName().start, ctx.qualifiedName().stop, options));

    MappingResult<Expression> whereResult = mapWhereClause(ctx.whereClause(), options);
    if (!whereResult.supported()) {
      return rawStatement(ctx, OracleStatementKind.DELETE, options);
    }

    return new ParseSuccess(
        SqlDialect.ORACLE,
        new DeleteStatement(
            target,
            whereResult.value(),
            List.of(),
            null,
            SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
        List.of(),
        ParseMetrics.unknown());
  }

  private static JoinType mapJoinKind(OracleParser.JoinKindContext ctx) {
    if (ctx == null) {
      return JoinType.INNER;
    }
    return JoinType.fromKind(ctx.LEFT() != null, ctx.RIGHT() != null, ctx.FULL() != null);
  }

  // =========================================================================
  // WHERE / GROUP BY / HAVING
  // =========================================================================

  private static MappingResult<Expression> mapWhereClause(
      OracleParser.WhereClauseContext ctx, ParseOptions options) {
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
      OracleParser.GroupByClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return new MappingResult<>(true, List.of());
    }
    List<Expression> items = new ArrayList<>();
    for (OracleParser.ExprContext exprCtx : ctx.expr()) {
      Expression expr = mapExpr(exprCtx, options);
      if (expr == null) {
        return new MappingResult<>(false, List.of());
      }
      items.add(expr);
    }
    return new MappingResult<>(true, List.copyOf(items));
  }

  private static MappingResult<Expression> mapHavingClause(
      OracleParser.HavingClauseContext ctx, ParseOptions options) {
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
  // ORDER BY
  // =========================================================================

  private static MappingResult<List<OrderByItem>> mapOrderByClause(
      OracleParser.OrderByClauseContext ctx, ParseOptions options) {
    if (ctx == null) {
      return new MappingResult<>(true, List.of());
    }
    List<OrderByItem> items = new ArrayList<>();
    for (OracleParser.OrderByItemContext item : ctx.orderByItem()) {
      if (item.NULLS() != null) {
        return new MappingResult<>(false, List.of());
      }
      Expression expr = mapExpr(item.expr(), options);
      if (expr == null) {
        return new MappingResult<>(false, List.of());
      }
      SortDirection dir = SortDirection.ASC;
      if (item.direction != null && item.direction.getType() == OracleParser.DESC) {
        dir = SortDirection.DESC;
      }
      items.add(new OrderByItem(expr, dir, SourceSpans.fromTokens(item.start, item.stop, options)));
    }
    return new MappingResult<>(true, List.copyOf(items));
  }

  // =========================================================================
  // Expression mapping
  // =========================================================================

  /**
   * Maps an expression context to an AST {@link Expression}. Returns null for expression shapes
   * that are outside the v1 subset; the caller should then fall back to a raw statement.
   */
  private static Expression mapExpr(OracleParser.ExprContext ctx, ParseOptions options) {
    if (ctx == null) {
      return null;
    }
    if (ctx instanceof OracleParser.LiteralExprContext literalCtx) {
      return mapLiteral(literalCtx.literal(), options);
    }
    if (ctx instanceof OracleParser.NameExprContext nameCtx) {
      return new IdentifierExpression(
          nameCtx.qualifiedName().getText(), SourceSpans.fromTokens(ctx.start, ctx.stop, options));
    }
    if (ctx instanceof OracleParser.ParenExprContext parenCtx) {
      return mapExpr(parenCtx.expr(), options);
    }
    if (ctx instanceof OracleParser.UnaryExprContext unaryCtx) {
      return mapUnaryExpr(unaryCtx, options);
    }
    if (ctx instanceof OracleParser.NotExprContext notCtx) {
      return mapNotExpr(notCtx, options);
    }
    if (ctx instanceof OracleParser.AndExprContext andCtx) {
      return mapBinaryExpr(andCtx.expr(0), andCtx.expr(1), BinaryOperator.AND, options);
    }
    if (ctx instanceof OracleParser.OrExprContext orCtx) {
      return mapBinaryExpr(orCtx.expr(0), orCtx.expr(1), BinaryOperator.OR, options);
    }
    if (ctx instanceof OracleParser.AddExprContext addCtx) {
      return mapAddExpr(addCtx, options);
    }
    if (ctx instanceof OracleParser.MulExprContext mulCtx) {
      return mapMulExpr(mulCtx, options);
    }
    if (ctx instanceof OracleParser.CompExprContext compCtx) {
      return mapCompExpr(compCtx, options);
    }
    if (ctx instanceof OracleParser.IsNullExprContext isNullCtx) {
      return mapIsNullExpr(isNullCtx, options);
    }
    // For any other expression shape, signal unsupported so caller can fall back to raw.
    return null;
  }

  private static Expression mapUnaryExpr(OracleParser.UnaryExprContext ctx, ParseOptions options) {
    int opType = ctx.op.getType();
    UnaryOperator op;
    if (opType == OracleParser.MINUS) {
      op = UnaryOperator.MINUS;
    } else if (opType == OracleParser.PLUS) {
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

  private static Expression mapNotExpr(OracleParser.NotExprContext ctx, ParseOptions options) {
    Expression operand = mapExpr(ctx.expr(), options);
    if (operand == null) {
      return null;
    }
    return new UnaryExpression(
        UnaryOperator.NOT, operand, SourceSpans.fromTokens(ctx.start, ctx.stop, options));
  }

  private static Expression mapAddExpr(OracleParser.AddExprContext ctx, ParseOptions options) {
    int opType = ctx.op.getType();
    BinaryOperator op;
    if (opType == OracleParser.PLUS) {
      op = BinaryOperator.PLUS;
    } else if (opType == OracleParser.MINUS) {
      op = BinaryOperator.MINUS;
    } else {
      // CONCAT operator (||) – fall back to raw
      return null;
    }
    return mapBinaryExpr(ctx.expr(0), ctx.expr(1), op, options);
  }

  private static Expression mapMulExpr(OracleParser.MulExprContext ctx, ParseOptions options) {
    int opType = ctx.op.getType();
    BinaryOperator op;
    if (opType == OracleParser.STAR) {
      op = BinaryOperator.MULTIPLY;
    } else {
      op = BinaryOperator.DIVIDE;
    }
    return mapBinaryExpr(ctx.expr(0), ctx.expr(1), op, options);
  }

  private static Expression mapCompExpr(OracleParser.CompExprContext ctx, ParseOptions options) {
    BinaryOperator op = mapCompOp(ctx.op.getType());
    if (op == null) {
      return null;
    }
    return mapBinaryExpr(ctx.expr(0), ctx.expr(1), op, options);
  }

  private static Expression mapIsNullExpr(
      OracleParser.IsNullExprContext ctx, ParseOptions options) {
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
      OracleParser.ExprContext left,
      OracleParser.ExprContext right,
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
      case OracleParser.EQ -> BinaryOperator.EQUAL;
      case OracleParser.NEQ -> BinaryOperator.NOT_EQUAL;
      case OracleParser.LT -> BinaryOperator.LESS_THAN;
      case OracleParser.LTE -> BinaryOperator.LESS_OR_EQUAL;
      case OracleParser.GT -> BinaryOperator.GREATER_THAN;
      case OracleParser.GTE -> BinaryOperator.GREATER_OR_EQUAL;
      default -> null;
    };
  }

  private static Expression mapLiteral(OracleParser.LiteralContext ctx, ParseOptions options) {
    if (ctx == null) {
      return null;
    }
    return new LiteralExpression(
        ctx.getText(), SourceSpans.fromTokens(ctx.start, ctx.stop, options));
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private static boolean hasUnsupportedSelectShape(OracleParser.SelectStatementContext ctx) {
    // CTEs not supported in normalized AST
    if (ctx.withClause() != null) {
      return true;
    }
    // SET operations (UNION, INTERSECT, MINUS) not supported
    OracleParser.QueryExpressionContext qe = ctx.queryExpression();
    if (!qe.setOperator().isEmpty()) {
      return true;
    }
    // Parenthesized sub-select at the top level not supported
    OracleParser.QueryTermContext term = qe.queryTerm(0);
    if (term.SELECT() == null) {
      return true;
    }
    // FETCH FIRST / OFFSET not supported in normalized AST
    return ctx.fetchClause() != null || ctx.offsetFetchClause() != null;
  }

  private static ParseResult rawStatement(
      org.antlr.v4.runtime.ParserRuleContext ctx, OracleStatementKind kind, ParseOptions options) {
    return new ParseSuccess(
        SqlDialect.ORACLE,
        new OracleRawStatement(
            kind, SourceSpans.textOf(ctx), SourceSpans.fromTokens(ctx.start, ctx.stop, options)),
        List.of(),
        ParseMetrics.unknown());
  }

  private static OracleStatementKind kindForStatement(OracleParser.StatementContext stmt) {
    if (stmt.selectStatement() != null) return OracleStatementKind.SELECT;
    if (stmt.insertStatement() != null) return OracleStatementKind.INSERT;
    if (stmt.updateStatement() != null) return OracleStatementKind.UPDATE;
    if (stmt.deleteStatement() != null) return OracleStatementKind.DELETE;
    if (stmt.createTableStatement() != null) return OracleStatementKind.CREATE_TABLE;
    if (stmt.dropTableStatement() != null) return OracleStatementKind.DROP_TABLE;
    if (stmt.truncateStatement() != null) return OracleStatementKind.TRUNCATE;
    if (stmt.commitStatement() != null) return OracleStatementKind.COMMIT;
    if (stmt.rollbackStatement() != null) return OracleStatementKind.ROLLBACK;
    if (stmt.savepointStatement() != null) return OracleStatementKind.SAVEPOINT;
    return OracleStatementKind.OTHER;
  }
}
