package io.github.e4c5.sqool.dialect.mysql;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.BetweenExpression;
import io.github.e4c5.sqool.ast.BinaryExpression;
import io.github.e4c5.sqool.ast.BinaryOperator;
import io.github.e4c5.sqool.ast.ColumnAssignment;
import io.github.e4c5.sqool.ast.ColumnDefinition;
import io.github.e4c5.sqool.ast.CreateDatabaseStatement;
import io.github.e4c5.sqool.ast.CreateTableStatement;
import io.github.e4c5.sqool.ast.DeleteStatement;
import io.github.e4c5.sqool.ast.DerivedTableReference;
import io.github.e4c5.sqool.ast.DropDatabaseStatement;
import io.github.e4c5.sqool.ast.DropTableStatement;
import io.github.e4c5.sqool.ast.Expression;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.FunctionCallExpression;
import io.github.e4c5.sqool.ast.IdentifierExpression;
import io.github.e4c5.sqool.ast.InExpression;
import io.github.e4c5.sqool.ast.InsertStatement;
import io.github.e4c5.sqool.ast.IsNullExpression;
import io.github.e4c5.sqool.ast.JoinTableReference;
import io.github.e4c5.sqool.ast.JoinType;
import io.github.e4c5.sqool.ast.LikeExpression;
import io.github.e4c5.sqool.ast.LimitClause;
import io.github.e4c5.sqool.ast.LiteralExpression;
import io.github.e4c5.sqool.ast.MySqlRawStatement;
import io.github.e4c5.sqool.ast.MySqlStatementKind;
import io.github.e4c5.sqool.ast.NamedTableReference;
import io.github.e4c5.sqool.ast.OrderByItem;
import io.github.e4c5.sqool.ast.ReplaceStatement;
import io.github.e4c5.sqool.ast.SelectItem;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.SetOperationStatement;
import io.github.e4c5.sqool.ast.SetOperator;
import io.github.e4c5.sqool.ast.ShowStatement;
import io.github.e4c5.sqool.ast.ShowStatementKind;
import io.github.e4c5.sqool.ast.SortDirection;
import io.github.e4c5.sqool.ast.SourceSpan;
import io.github.e4c5.sqool.ast.SqlScript;
import io.github.e4c5.sqool.ast.Statement;
import io.github.e4c5.sqool.ast.TableReference;
import io.github.e4c5.sqool.ast.TruncateTableStatement;
import io.github.e4c5.sqool.ast.UnaryExpression;
import io.github.e4c5.sqool.ast.UnaryOperator;
import io.github.e4c5.sqool.ast.UpdateStatement;
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
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

final class MysqlAstMapper {
  private static final Pattern SUPPORTED_IDENTIFIER =
      Pattern.compile("(?i)(`[^`]+`|[a-z_][a-z0-9_$]*)(\\.(?:`[^`]+`|[a-z_][a-z0-9_$]*)){0,2}");

  private MysqlAstMapper() {}

  static ParseResult mapQueries(MySQLParser.QueriesContext context, ParseOptions options) {
    try {
      var statements = new ArrayList<Statement>();
      for (var query : context.query()) {
        statements.add(mapQuery(query, options));
      }
      SourceSpan sourceSpan =
          statements.isEmpty()
              ? null
              : span(context.query().getFirst().start, context.query().getLast().stop, options);
      return new ParseSuccess(SqlDialect.MYSQL, new SqlScript(statements, sourceSpan), List.of());
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

  static ParseResult mapSimpleStatement(
      MySQLParser.SimpleStatementContext context, ParseOptions options) {
    try {
      return new ParseSuccess(
          SqlDialect.MYSQL, mapSimpleStatementInternal(context, options), List.of());
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

  static ParseResult mapQueryExpression(
      MySQLParser.QueryExpressionContext context, ParseOptions options) {
    try {
      return new ParseSuccess(
          SqlDialect.MYSQL, mapQueryExpressionInternal(context, options), List.of());
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

    return new QuerySpecificationMapping(
        mapDistinct(context.selectOption()),
        mapSelectItems(context.selectItemList(), options),
        context.fromClause() == null ? null : mapFromClause(context.fromClause(), options),
        context.whereClause() == null ? null : mapExpr(context.whereClause().expr(), options),
        mapGroupBy(context.groupByClause(), options),
        context.havingClause() == null ? null : mapExpr(context.havingClause().expr(), options));
  }

  private static Statement mapQuery(MySQLParser.QueryContext context, ParseOptions options) {
    if (context.beginWork() != null) {
      return rawStatement(MySqlStatementKind.BEGIN_WORK, context.beginWork(), options);
    }
    if (context.simpleStatement() == null) {
      throw unsupportedFeature(
          "MySQL script mode encountered an unsupported statement.", context.start);
    }
    return mapSimpleStatementInternal(context.simpleStatement(), options);
  }

  private static Statement mapSimpleStatementInternal(
      MySQLParser.SimpleStatementContext context, ParseOptions options) {
    try {
      if (context.selectStatement() != null) {
        return mapSelectStatement(context.selectStatement(), options);
      }
      if (context.insertStatement() != null) {
        return mapInsertStatement(context.insertStatement(), options);
      }
      if (context.updateStatement() != null) {
        return mapUpdateStatement(context.updateStatement(), options);
      }
      if (context.deleteStatement() != null) {
        return mapDeleteStatement(context.deleteStatement(), options);
      }
      if (context.replaceStatement() != null) {
        return mapReplaceStatement(context.replaceStatement(), options);
      }
      if (context.truncateTableStatement() != null) {
        return mapTruncateTableStatement(context.truncateTableStatement(), options);
      }
      if (context.createStatement() != null) {
        if (context.createStatement().createTable() != null) {
          return mapCreateTableStatement(context.createStatement().createTable(), options);
        }
        if (context.createStatement().createDatabase() != null) {
          return mapCreateDatabaseStatement(context.createStatement().createDatabase(), options);
        }
        return rawStatement(MySqlStatementKind.CREATE_OTHER, context.createStatement(), options);
      }
      if (context.dropStatement() != null) {
        return mapDropStatement(context.dropStatement(), options);
      }
      if (context.showDatabasesStatement() != null) {
        return mapShowDatabasesStatement(context.showDatabasesStatement(), options);
      }
      if (context.showTablesStatement() != null) {
        return mapShowTablesStatement(context.showTablesStatement(), options);
      }
      if (context.showColumnsStatement() != null) {
        return mapShowColumnsStatement(context.showColumnsStatement(), options);
      }
      if (context.showCreateTableStatement() != null) {
        return mapShowCreateTableStatement(context.showCreateTableStatement(), options);
      }
      if (context.alterStatement() != null) {
        return rawStatement(MySqlStatementKind.ALTER, context.alterStatement(), options);
      }
      if (context.renameTableStatement() != null) {
        return rawStatement(
            MySqlStatementKind.RENAME_TABLE, context.renameTableStatement(), options);
      }
      if (context.importStatement() != null) {
        return rawStatement(MySqlStatementKind.IMPORT, context.importStatement(), options);
      }
      if (context.callStatement() != null) {
        return rawStatement(MySqlStatementKind.CALL, context.callStatement(), options);
      }
      if (context.doStatement() != null) {
        return rawStatement(MySqlStatementKind.DO, context.doStatement(), options);
      }
      if (context.handlerStatement() != null) {
        return rawStatement(MySqlStatementKind.HANDLER, context.handlerStatement(), options);
      }
      if (context.loadStatement() != null) {
        return rawStatement(MySqlStatementKind.LOAD, context.loadStatement(), options);
      }
      if (context.transactionOrLockingStatement() != null) {
        return rawStatement(
            MySqlStatementKind.TRANSACTION_OR_LOCKING,
            context.transactionOrLockingStatement(),
            options);
      }
      if (context.replicationStatement() != null) {
        return rawStatement(
            MySqlStatementKind.REPLICATION, context.replicationStatement(), options);
      }
      if (context.preparedStatement() != null) {
        return rawStatement(MySqlStatementKind.PREPARED, context.preparedStatement(), options);
      }
      if (context.cloneStatement() != null) {
        return rawStatement(MySqlStatementKind.CLONE, context.cloneStatement(), options);
      }
      if (context.accountManagementStatement() != null) {
        return rawStatement(
            MySqlStatementKind.ACCOUNT_MANAGEMENT, context.accountManagementStatement(), options);
      }
      if (context.tableAdministrationStatement() != null) {
        return rawStatement(
            MySqlStatementKind.TABLE_ADMINISTRATION,
            context.tableAdministrationStatement(),
            options);
      }
      if (context.uninstallStatement() != null) {
        return rawStatement(MySqlStatementKind.UNINSTALL, context.uninstallStatement(), options);
      }
      if (context.installStatement() != null) {
        return rawStatement(MySqlStatementKind.INSTALL, context.installStatement(), options);
      }
      if (context.setStatement() != null) {
        return rawStatement(MySqlStatementKind.SET, context.setStatement(), options);
      }
      if (context.resourceGroupManagement() != null) {
        return rawStatement(
            MySqlStatementKind.OTHER_ADMINISTRATIVE, context.resourceGroupManagement(), options);
      }
      if (context.otherAdministrativeStatement() != null) {
        return rawStatement(
            MySqlStatementKind.OTHER_ADMINISTRATIVE,
            context.otherAdministrativeStatement(),
            options);
      }
      if (context.utilityStatement() != null) {
        return rawStatement(MySqlStatementKind.UTILITY, context.utilityStatement(), options);
      }
      if (context.getDiagnosticsStatement() != null) {
        return rawStatement(
            MySqlStatementKind.GET_DIAGNOSTICS, context.getDiagnosticsStatement(), options);
      }
      if (context.signalStatement() != null) {
        return rawStatement(MySqlStatementKind.SIGNAL, context.signalStatement(), options);
      }
      if (context.resignalStatement() != null) {
        return rawStatement(MySqlStatementKind.RESIGNAL, context.resignalStatement(), options);
      }
      if (context.showTriggersStatement() != null
          || context.showEventsStatement() != null
          || context.showTableStatusStatement() != null
          || context.showOpenTablesStatement() != null
          || context.showParseTreeStatement() != null
          || context.showPluginsStatement() != null
          || context.showEngineLogsStatement() != null
          || context.showEngineMutexStatement() != null
          || context.showEngineStatusStatement() != null
          || context.showBinaryLogsStatement() != null
          || context.showBinaryLogStatusStatement() != null
          || context.showReplicasStatement() != null
          || context.showBinlogEventsStatement() != null
          || context.showRelaylogEventsStatement() != null
          || context.showKeysStatement() != null
          || context.showEnginesStatement() != null
          || context.showCountWarningsStatement() != null
          || context.showCountErrorsStatement() != null
          || context.showWarningsStatement() != null
          || context.showErrorsStatement() != null
          || context.showProfilesStatement() != null
          || context.showProfileStatement() != null
          || context.showStatusStatement() != null
          || context.showProcessListStatement() != null
          || context.showVariablesStatement() != null
          || context.showCharacterSetStatement() != null
          || context.showCollationStatement() != null
          || context.showPrivilegesStatement() != null
          || context.showGrantsStatement() != null
          || context.showCreateDatabaseStatement() != null
          || context.showCreateViewStatement() != null
          || context.showMasterStatusStatement() != null
          || context.showReplicaStatusStatement() != null
          || context.showCreateProcedureStatement() != null
          || context.showCreateFunctionStatement() != null
          || context.showCreateTriggerStatement() != null
          || context.showCreateProcedureStatusStatement() != null
          || context.showCreateFunctionStatusStatement() != null
          || context.showCreateProcedureCodeStatement() != null
          || context.showCreateFunctionCodeStatement() != null
          || context.showCreateEventStatement() != null
          || context.showCreateUserStatement() != null) {
        return rawStatement(MySqlStatementKind.SHOW_OTHER, context, options);
      }
      throw unsupportedFeature(
          "MySQL MVP does not support this statement kind yet.", context.start);
    } catch (UnsupportedFeatureException exception) {
      return rawSimpleStatement(context, options);
    }
  }

  private static Statement mapSelectStatement(
      MySQLParser.SelectStatementContext context, ParseOptions options) {
    if (context.lockingClauseList() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support SELECT locking clauses yet.",
          context.lockingClauseList().start);
    }
    if (context.selectStatementWithInto() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support SELECT ... INTO forms yet.",
          context.selectStatementWithInto().start);
    }
    return mapQueryExpressionInternal(context.queryExpression(), options);
  }

  private static Statement mapInsertStatement(
      MySQLParser.InsertStatementContext context, ParseOptions options) {
    if (context.insertLockOption() != null || context.usePartition() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support INSERT lock or partition options yet.", context.start);
    }

    String tableName = context.tableRef().getText();
    boolean ignore = context.IGNORE_SYMBOL() != null;
    List<String> columns = List.of();
    List<List<Expression>> rows = List.of();
    List<ColumnAssignment> assignments = List.of();
    Statement sourceQuery = null;

    if (context.insertFromConstructor() != null) {
      columns = mapFields(context.insertFromConstructor().fields());
      rows = mapValueList(context.insertFromConstructor().insertValues().valueList(), options);
      if (context.valuesReference() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support VALUES references in INSERT yet.",
            context.valuesReference().start);
      }
    } else if (context.updateList() != null) {
      assignments = mapUpdateList(context.updateList(), options);
      if (context.valuesReference() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support VALUES references in INSERT ... SET yet.",
            context.valuesReference().start);
      }
    } else if (context.insertQueryExpression() != null) {
      columns = mapFields(context.insertQueryExpression().fields());
      sourceQuery = mapInsertQueryExpression(context.insertQueryExpression(), options);
    } else {
      throw unsupportedFeature("MySQL MVP encountered an unsupported INSERT shape.", context.start);
    }

    return new InsertStatement(
        tableName,
        columns,
        rows,
        assignments,
        sourceQuery,
        context.insertUpdateList() == null
            ? List.of()
            : mapUpdateList(context.insertUpdateList().updateList(), options),
        ignore,
        span(context.start, context.stop, options));
  }

  private static Statement mapUpdateStatement(
      MySQLParser.UpdateStatementContext context, ParseOptions options) {
    if (context.withClause() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support UPDATE with WITH clauses yet.", context.start);
    }
    if (context.tableReferenceList().tableReference().size() != 1
        || !context.tableReferenceList().tableReference().getFirst().joinedTable().isEmpty()) {
      throw unsupportedFeature(
          "MySQL MVP currently supports only single-table UPDATE statements.", context.start);
    }

    return new UpdateStatement(
        mapTableReference(context.tableReferenceList().tableReference().getFirst(), options),
        mapUpdateList(context.updateList(), options),
        context.whereClause() == null ? null : mapExpr(context.whereClause().expr(), options),
        mapOrderBy(context.orderClause(), options),
        mapSimpleLimitClause(context.simpleLimitClause(), options),
        context.IGNORE_SYMBOL() != null,
        span(context.start, context.stop, options));
  }

  private static Statement mapDeleteStatement(
      MySQLParser.DeleteStatementContext context, ParseOptions options) {
    if (context.withClause() != null
        || context.tableAliasRefList() != null
        || context.USING_SYMBOL() != null
        || context.partitionDelete() != null
        || !context.deleteStatementOption().isEmpty()) {
      throw unsupportedFeature(
          "MySQL MVP currently supports only simple single-table DELETE statements.",
          context.start);
    }
    if (context.tableRef() == null) {
      throw unsupportedFeature("MySQL MVP encountered an unsupported DELETE shape.", context.start);
    }

    return new DeleteStatement(
        new NamedTableReference(
            context.tableRef().getText(),
            aliasText(context.tableAlias()),
            span(context.tableRef().start, context.tableRef().stop, options)),
        context.whereClause() == null ? null : mapExpr(context.whereClause().expr(), options),
        mapOrderBy(context.orderClause(), options),
        mapSimpleLimitClause(context.simpleLimitClause(), options),
        span(context.start, context.stop, options));
  }

  private static Statement mapReplaceStatement(
      MySQLParser.ReplaceStatementContext context, ParseOptions options) {
    if (context.usePartition() != null
        || context.LOW_PRIORITY_SYMBOL() != null
        || context.DELAYED_SYMBOL() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support REPLACE priority or partition options yet.", context.start);
    }

    String tableName = context.tableRef().getText();
    List<String> columns = List.of();
    List<List<Expression>> rows = List.of();
    List<ColumnAssignment> assignments = List.of();
    Statement sourceQuery = null;

    if (context.insertFromConstructor() != null) {
      columns = mapFields(context.insertFromConstructor().fields());
      rows = mapValueList(context.insertFromConstructor().insertValues().valueList(), options);
    } else if (context.updateList() != null) {
      assignments = mapUpdateList(context.updateList(), options);
    } else if (context.insertQueryExpression() != null) {
      columns = mapFields(context.insertQueryExpression().fields());
      sourceQuery = mapInsertQueryExpression(context.insertQueryExpression(), options);
    } else {
      throw unsupportedFeature(
          "MySQL MVP encountered an unsupported REPLACE shape.", context.start);
    }

    return new ReplaceStatement(
        tableName,
        columns,
        rows,
        assignments,
        sourceQuery,
        span(context.start, context.stop, options));
  }

  private static Statement mapTruncateTableStatement(
      MySQLParser.TruncateTableStatementContext context, ParseOptions options) {
    return new TruncateTableStatement(
        context.tableRef().getText(), span(context.start, context.stop, options));
  }

  private static Statement mapCreateTableStatement(
      MySQLParser.CreateTableContext context, ParseOptions options) {
    String tableName = context.tableName().getText();
    boolean temporary = context.TEMPORARY_SYMBOL() != null;
    boolean ifNotExists = context.ifNotExists() != null;

    if (context.LIKE_SYMBOL() != null && context.tableRef() != null) {
      return new CreateTableStatement(
          tableName,
          temporary,
          ifNotExists,
          List.of(),
          context.tableRef().getText(),
          null,
          span(context.start, context.stop, options));
    }

    if (context.tableElementList() == null) {
      throw unsupportedFeature(
          "MySQL MVP encountered an unsupported CREATE TABLE form.", context.start);
    }

    var columns = new ArrayList<ColumnDefinition>();
    for (var tableElement : context.tableElementList().tableElement()) {
      if (tableElement.tableConstraintDef() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support table constraints in CREATE TABLE yet.",
            tableElement.tableConstraintDef().start);
      }
      columns.add(mapColumnDefinition(tableElement.columnDefinition(), options));
    }

    return new CreateTableStatement(
        tableName,
        temporary,
        ifNotExists,
        columns,
        null,
        context.createTableOptionsEtc() == null ? null : context.createTableOptionsEtc().getText(),
        span(context.start, context.stop, options));
  }

  private static Statement mapCreateDatabaseStatement(
      MySQLParser.CreateDatabaseContext context, ParseOptions options) {
    if (!context.createDatabaseOption().isEmpty()) {
      throw unsupportedFeature(
          "MySQL MVP does not support CREATE DATABASE options yet.", context.start);
    }
    return new CreateDatabaseStatement(
        context.schemaName().getText(),
        context.ifNotExists() != null,
        span(context.start, context.stop, options));
  }

  private static Statement mapDropStatement(
      MySQLParser.DropStatementContext context, ParseOptions options) {
    if (context.dropTable() != null) {
      return mapDropTableStatement(context.dropTable(), options);
    }
    if (context.dropDatabase() != null) {
      return mapDropDatabaseStatement(context.dropDatabase(), options);
    }
    throw unsupportedFeature(
        "MySQL MVP does not support this DROP statement kind yet.", context.start);
  }

  private static Statement mapDropTableStatement(
      MySQLParser.DropTableContext context, ParseOptions options) {
    if (context.CASCADE_SYMBOL() != null || context.RESTRICT_SYMBOL() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support DROP TABLE CASCADE/RESTRICT yet.", context.start);
    }
    var tableNames = new ArrayList<String>();
    for (var tableRef : context.tableRefList().tableRef()) {
      tableNames.add(tableRef.getText());
    }
    return new DropTableStatement(
        tableNames,
        context.ifExists() != null,
        context.TEMPORARY_SYMBOL() != null,
        span(context.start, context.stop, options));
  }

  private static Statement mapDropDatabaseStatement(
      MySQLParser.DropDatabaseContext context, ParseOptions options) {
    return new DropDatabaseStatement(
        context.schemaRef().getText(),
        context.ifExists() != null,
        span(context.start, context.stop, options));
  }

  private static Statement mapQueryExpressionInternal(
      MySQLParser.QueryExpressionContext context, ParseOptions options) {
    if (context.withClause() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support WITH clauses yet.", context.withClause().start);
    }

    Statement statement = mapQueryExpressionBody(context.queryExpressionBody(), options);
    if (context.orderClause() != null || context.limitClause() != null) {
      statement =
          withOrderAndLimit(
              statement,
              mapOrderBy(context.orderClause(), options),
              mapLimitClause(context.limitClause(), options),
              span(context.start, context.stop, options));
    }
    return statement;
  }

  private static Statement mapQueryExpressionBody(
      MySQLParser.QueryExpressionBodyContext context, ParseOptions options) {
    Statement current;
    if (context.queryPrimary() != null) {
      current = mapQueryPrimary(context.queryPrimary(), options);
    } else if (context.queryExpressionParens() != null) {
      current = mapQueryExpressionParens(context.queryExpressionParens(), options);
    } else {
      throw unsupportedFeature(
          "MySQL MVP encountered an unsupported query expression body.", context.start);
    }

    if (context.children == null) {
      return current;
    }

    SetOperator pendingOperator = null;
    boolean skippedBase = false;
    for (var child : context.children) {
      if (!skippedBase
          && (child == context.queryPrimary() || child == context.queryExpressionParens())) {
        skippedBase = true;
        continue;
      }

      if (child instanceof TerminalNode terminalNode) {
        int tokenType = terminalNode.getSymbol().getType();
        if (tokenType == MySQLParser.UNION_SYMBOL) {
          pendingOperator = SetOperator.UNION_DISTINCT;
        } else if (tokenType == MySQLParser.EXCEPT_SYMBOL
            || tokenType == MySQLParser.INTERSECT_SYMBOL) {
          throw unsupportedFeature(
              "MySQL MVP does not support EXCEPT or INTERSECT yet.", terminalNode.getSymbol());
        }
        continue;
      }

      if (child instanceof MySQLParser.UnionOptionContext unionOptionContext) {
        pendingOperator =
            "ALL".equalsIgnoreCase(unionOptionContext.getText())
                ? SetOperator.UNION_ALL
                : SetOperator.UNION_DISTINCT;
        continue;
      }

      if (child instanceof MySQLParser.QueryExpressionBodyContext rhsBody) {
        current =
            new SetOperationStatement(
                current,
                pendingOperator == null ? SetOperator.UNION_DISTINCT : pendingOperator,
                mapQueryExpressionBody(rhsBody, options),
                List.of(),
                null,
                span(context.start, context.stop, options));
        pendingOperator = null;
      }
    }

    return current;
  }

  private static Statement mapQueryPrimary(
      MySQLParser.QueryPrimaryContext context, ParseOptions options) {
    if (context.querySpecification() != null) {
      var mappedBody = mapQuerySpecification(context.querySpecification(), options);
      return new SelectStatement(
          mappedBody.distinct(),
          mappedBody.selectItems(),
          mappedBody.from(),
          mappedBody.where(),
          mappedBody.groupBy(),
          mappedBody.having(),
          List.of(),
          null,
          span(context.start, context.stop, options));
    }
    throw unsupportedFeature(
        "MySQL MVP currently supports only SELECT query primaries.", context.start);
  }

  private static Statement mapQueryExpressionParens(
      MySQLParser.QueryExpressionParensContext context, ParseOptions options) {
    if (context.queryExpressionParens() != null) {
      return mapQueryExpressionParens(context.queryExpressionParens(), options);
    }
    if (context.queryExpressionWithOptLockingClauses() == null) {
      throw unsupportedFeature(
          "MySQL MVP encountered an unsupported parenthesized query expression.", context.start);
    }
    if (context.queryExpressionWithOptLockingClauses().lockingClauseList() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support locking clauses inside subqueries yet.",
          context.queryExpressionWithOptLockingClauses().lockingClauseList().start);
    }
    return mapQueryExpressionInternal(
        context.queryExpressionWithOptLockingClauses().queryExpression(), options);
  }

  private static Statement withOrderAndLimit(
      Statement statement, List<OrderByItem> orderBy, LimitClause limit, SourceSpan sourceSpan) {
    if (statement instanceof SelectStatement selectStatement) {
      return new SelectStatement(
          selectStatement.distinct(),
          selectStatement.selectItems(),
          selectStatement.from(),
          selectStatement.where(),
          selectStatement.groupBy(),
          selectStatement.having(),
          orderBy,
          limit,
          sourceSpan);
    }
    if (statement instanceof SetOperationStatement setOperationStatement) {
      return new SetOperationStatement(
          setOperationStatement.left(),
          setOperationStatement.operator(),
          setOperationStatement.right(),
          orderBy,
          limit,
          sourceSpan);
    }
    return statement;
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
    if (context.derivedTable() != null) {
      return mapDerivedTable(context.derivedTable(), options);
    }
    if (context.tableReferenceListParens() != null) {
      return mapTableReferenceListParens(context.tableReferenceListParens(), options);
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
      return new JoinTableReference(
          left,
          mapJoinType(context),
          mapTableReference(context.tableReference(), options),
          null,
          mapIdentifierListWithParentheses(context.identifierListWithParentheses()),
          span(context.start, context.stop, options));
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
        List.of(),
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

  private static List<String> mapFields(MySQLParser.FieldsContext context) {
    if (context == null) {
      return List.of();
    }
    var columns = new ArrayList<String>();
    for (var insertIdentifier : context.insertIdentifier()) {
      if (insertIdentifier.tableWild() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support wildcard INSERT target lists.", insertIdentifier.start);
      }
      columns.add(insertIdentifier.columnRef().getText());
    }
    return List.copyOf(columns);
  }

  private static List<List<Expression>> mapValueList(
      MySQLParser.ValueListContext context, ParseOptions options) {
    var rows = new ArrayList<List<Expression>>();
    for (var values : context.values()) {
      rows.add(mapValues(values, options));
    }
    if (rows.isEmpty()) {
      rows.add(List.of());
    }
    return List.copyOf(rows);
  }

  private static List<Expression> mapValues(
      MySQLParser.ValuesContext context, ParseOptions options) {
    var values = new ArrayList<Expression>();
    if (context.children == null) {
      return List.of();
    }
    for (var child : context.children) {
      if (child instanceof MySQLParser.ExprContext exprContext) {
        values.add(mapExpr(exprContext, options));
      } else if (child instanceof TerminalNode terminalNode
          && terminalNode.getSymbol().getType() == MySQLParser.DEFAULT_SYMBOL) {
        values.add(
            new LiteralExpression(
                "DEFAULT", span(terminalNode.getSymbol(), terminalNode.getSymbol(), options)));
      }
    }
    return List.copyOf(values);
  }

  private static List<ColumnAssignment> mapUpdateList(
      MySQLParser.UpdateListContext context, ParseOptions options) {
    var assignments = new ArrayList<ColumnAssignment>();
    for (var updateElement : context.updateElement()) {
      assignments.add(
          new ColumnAssignment(
              updateElement.columnRef().getText(),
              updateElement.DEFAULT_SYMBOL() != null
                  ? new LiteralExpression(
                      "DEFAULT",
                      span(
                          updateElement.DEFAULT_SYMBOL().getSymbol(),
                          updateElement.DEFAULT_SYMBOL().getSymbol(),
                          options))
                  : mapExpr(updateElement.expr(), options),
              span(updateElement.start, updateElement.stop, options)));
    }
    return List.copyOf(assignments);
  }

  private static LimitClause mapSimpleLimitClause(
      MySQLParser.SimpleLimitClauseContext context, ParseOptions options) {
    if (context == null) {
      return null;
    }
    return new LimitClause(
        numericLimit(context.limitOption()), null, span(context.start, context.stop, options));
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

  private static Statement mapInsertQueryExpression(
      MySQLParser.InsertQueryExpressionContext context, ParseOptions options) {
    if (context.queryExpression() != null) {
      return mapQueryExpressionInternal(context.queryExpression(), options);
    }
    if (context.queryExpressionParens() != null) {
      return mapQueryExpressionParens(context.queryExpressionParens(), options);
    }
    if (context.queryExpressionWithOptLockingClauses() != null) {
      if (context.queryExpressionWithOptLockingClauses().lockingClauseList() != null) {
        throw unsupportedFeature(
            "MySQL MVP does not support locking clauses in INSERT ... SELECT yet.",
            context.queryExpressionWithOptLockingClauses().lockingClauseList().start);
      }
      return mapQueryExpressionInternal(
          context.queryExpressionWithOptLockingClauses().queryExpression(), options);
    }
    throw unsupportedFeature(
        "MySQL MVP encountered an unsupported INSERT ... SELECT form.", context.start);
  }

  private static Statement mapShowDatabasesStatement(
      MySQLParser.ShowDatabasesStatementContext context, ParseOptions options) {
    return new ShowStatement(
        ShowStatementKind.SHOW_DATABASES,
        null,
        null,
        null,
        context.likeOrWhere() == null ? null : context.likeOrWhere().getText(),
        span(context.start, context.stop, options));
  }

  private static Statement mapShowTablesStatement(
      MySQLParser.ShowTablesStatementContext context, ParseOptions options) {
    return new ShowStatement(
        ShowStatementKind.SHOW_TABLES,
        null,
        context.inDb() == null ? null : context.inDb().identifier().getText(),
        context.showCommandType() == null
            ? null
            : context.showCommandType().getText().toUpperCase(Locale.ROOT),
        context.likeOrWhere() == null ? null : context.likeOrWhere().getText(),
        span(context.start, context.stop, options));
  }

  private static Statement mapShowColumnsStatement(
      MySQLParser.ShowColumnsStatementContext context, ParseOptions options) {
    return new ShowStatement(
        ShowStatementKind.SHOW_COLUMNS,
        context.tableRef().getText(),
        context.inDb() == null ? null : context.inDb().identifier().getText(),
        context.showCommandType() == null
            ? null
            : context.showCommandType().getText().toUpperCase(Locale.ROOT),
        context.likeOrWhere() == null ? null : context.likeOrWhere().getText(),
        span(context.start, context.stop, options));
  }

  private static Statement mapShowCreateTableStatement(
      MySQLParser.ShowCreateTableStatementContext context, ParseOptions options) {
    return new ShowStatement(
        ShowStatementKind.SHOW_CREATE_TABLE,
        context.tableRef().getText(),
        null,
        null,
        null,
        span(context.start, context.stop, options));
  }

  private static Statement rawSimpleStatement(
      MySQLParser.SimpleStatementContext context, ParseOptions options) {
    return rawStatement(kindForSimpleStatement(context), context, options);
  }

  private static Statement rawStatement(
      MySqlStatementKind kind,
      org.antlr.v4.runtime.ParserRuleContext context,
      ParseOptions options) {
    var input = context.start == null ? null : context.start.getInputStream();
    String sql =
        input == null || context.stop == null
            ? context.getText()
            : input.getText(
                Interval.of(context.start.getStartIndex(), context.stop.getStopIndex()));
    return new MySqlRawStatement(kind, sql, span(context.start, context.stop, options));
  }

  private static MySqlStatementKind kindForSimpleStatement(
      MySQLParser.SimpleStatementContext context) {
    if (context.selectStatement() != null) {
      return MySqlStatementKind.SELECT;
    }
    if (context.insertStatement() != null) {
      return MySqlStatementKind.INSERT;
    }
    if (context.updateStatement() != null) {
      return MySqlStatementKind.UPDATE;
    }
    if (context.deleteStatement() != null) {
      return MySqlStatementKind.DELETE;
    }
    if (context.replaceStatement() != null) {
      return MySqlStatementKind.REPLACE;
    }
    if (context.truncateTableStatement() != null) {
      return MySqlStatementKind.TRUNCATE_TABLE;
    }
    if (context.createStatement() != null) {
      if (context.createStatement().createTable() != null) {
        return MySqlStatementKind.CREATE_TABLE;
      }
      if (context.createStatement().createDatabase() != null) {
        return MySqlStatementKind.CREATE_DATABASE;
      }
      return MySqlStatementKind.CREATE_OTHER;
    }
    if (context.dropStatement() != null) {
      if (context.dropStatement().dropTable() != null) {
        return MySqlStatementKind.DROP_TABLE;
      }
      if (context.dropStatement().dropDatabase() != null) {
        return MySqlStatementKind.DROP_DATABASE;
      }
      return MySqlStatementKind.DROP_OTHER;
    }
    if (context.showDatabasesStatement() != null) {
      return MySqlStatementKind.SHOW_DATABASES;
    }
    if (context.showTablesStatement() != null) {
      return MySqlStatementKind.SHOW_TABLES;
    }
    if (context.showColumnsStatement() != null) {
      return MySqlStatementKind.SHOW_COLUMNS;
    }
    if (context.showCreateTableStatement() != null) {
      return MySqlStatementKind.SHOW_CREATE_TABLE;
    }
    if (context.alterStatement() != null) {
      return MySqlStatementKind.ALTER;
    }
    if (context.renameTableStatement() != null) {
      return MySqlStatementKind.RENAME_TABLE;
    }
    if (context.importStatement() != null) {
      return MySqlStatementKind.IMPORT;
    }
    if (context.callStatement() != null) {
      return MySqlStatementKind.CALL;
    }
    if (context.doStatement() != null) {
      return MySqlStatementKind.DO;
    }
    if (context.handlerStatement() != null) {
      return MySqlStatementKind.HANDLER;
    }
    if (context.loadStatement() != null) {
      return MySqlStatementKind.LOAD;
    }
    if (context.transactionOrLockingStatement() != null) {
      return MySqlStatementKind.TRANSACTION_OR_LOCKING;
    }
    if (context.replicationStatement() != null) {
      return MySqlStatementKind.REPLICATION;
    }
    if (context.preparedStatement() != null) {
      return MySqlStatementKind.PREPARED;
    }
    if (context.cloneStatement() != null) {
      return MySqlStatementKind.CLONE;
    }
    if (context.accountManagementStatement() != null) {
      return MySqlStatementKind.ACCOUNT_MANAGEMENT;
    }
    if (context.tableAdministrationStatement() != null) {
      return MySqlStatementKind.TABLE_ADMINISTRATION;
    }
    if (context.uninstallStatement() != null) {
      return MySqlStatementKind.UNINSTALL;
    }
    if (context.installStatement() != null) {
      return MySqlStatementKind.INSTALL;
    }
    if (context.setStatement() != null) {
      return MySqlStatementKind.SET;
    }
    if (context.resourceGroupManagement() != null
        || context.otherAdministrativeStatement() != null) {
      return MySqlStatementKind.OTHER_ADMINISTRATIVE;
    }
    if (context.utilityStatement() != null) {
      return MySqlStatementKind.UTILITY;
    }
    if (context.getDiagnosticsStatement() != null) {
      return MySqlStatementKind.GET_DIAGNOSTICS;
    }
    if (context.signalStatement() != null) {
      return MySqlStatementKind.SIGNAL;
    }
    if (context.resignalStatement() != null) {
      return MySqlStatementKind.RESIGNAL;
    }
    return MySqlStatementKind.SHOW_OTHER;
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
      return new IsNullExpression(
          mapBoolPri(isNullContext.boolPri(), options),
          isNullContext.notRule() != null,
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
      case "/" -> BinaryOperator.DIVIDE;
      case "DIV" -> BinaryOperator.INTEGER_DIVIDE;
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

  private static ColumnDefinition mapColumnDefinition(
      MySQLParser.ColumnDefinitionContext context, ParseOptions options) {
    if (context.fieldDefinition().AS_SYMBOL() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support generated columns in CREATE TABLE yet.", context.start);
    }
    if (context.checkOrReferences() != null) {
      throw unsupportedFeature(
          "MySQL MVP does not support column-level CHECK or REFERENCES clauses yet.",
          context.checkOrReferences().start);
    }

    var attributes = new ArrayList<String>();
    for (var attribute : context.fieldDefinition().columnAttribute()) {
      attributes.add(attribute.getText());
    }

    return new ColumnDefinition(
        context.columnName().getText(),
        context.fieldDefinition().dataType().getText(),
        attributes,
        span(context.start, context.stop, options));
  }

  private static DerivedTableReference mapDerivedTable(
      MySQLParser.DerivedTableContext context, ParseOptions options) {
    if (context.getStart().getType() == MySQLParser.LATERAL_SYMBOL) {
      throw unsupportedFeature(
          "MySQL MVP does not support LATERAL derived tables yet.", context.start);
    }
    if (context.subquery() == null) {
      throw unsupportedFeature(
          "MySQL MVP encountered an unsupported derived table form.", context.start);
    }

    return new DerivedTableReference(
        mapQueryExpressionParens(context.subquery().queryExpressionParens(), options),
        aliasText(context.tableAlias()),
        mapColumnAliases(context.columnInternalRefList()),
        span(context.start, context.stop, options));
  }

  private static TableReference mapTableReferenceListParens(
      MySQLParser.TableReferenceListParensContext context, ParseOptions options) {
    if (context.tableReferenceListParens() != null) {
      return mapTableReferenceListParens(context.tableReferenceListParens(), options);
    }
    if (context.tableReferenceList() == null
        || context.tableReferenceList().tableReference().size() != 1) {
      throw unsupportedFeature(
          "MySQL MVP currently supports only a single table reference inside parenthesized FROM items.",
          context.start);
    }
    return mapTableReference(context.tableReferenceList().tableReference().getFirst(), options);
  }

  private static List<String> mapIdentifierListWithParentheses(
      MySQLParser.IdentifierListWithParenthesesContext context) {
    var identifiers = new ArrayList<String>();
    for (var identifier : context.identifierList().identifier()) {
      identifiers.add(identifier.getText());
    }
    return List.copyOf(identifiers);
  }

  private static List<String> mapColumnAliases(MySQLParser.ColumnInternalRefListContext context) {
    if (context == null) {
      return List.of();
    }
    var aliases = new ArrayList<String>();
    for (var columnInternalRef : context.columnInternalRef()) {
      aliases.add(columnInternalRef.identifier().getText());
    }
    return List.copyOf(aliases);
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

    int startColumn = start.getCharPositionInLine();
    int stopColumn = stop.getCharPositionInLine();
    if (stop.getText() != null && !stop.getText().isEmpty()) {
      stopColumn += stop.getText().length() - 1;
    }
    if (start.getLine() == stop.getLine()) {
      stopColumn = Math.max(stopColumn, startColumn);
    }

    return new SourceSpan(
        start.getStartIndex(),
        stop.getStopIndex(),
        start.getLine(),
        startColumn,
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
