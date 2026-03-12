package io.github.e4c5.sqool.dialect.mysql;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.ExpressionSelectItem;
import io.github.e4c5.sqool.ast.IdentifierExpression;
import io.github.e4c5.sqool.ast.NamedTableReference;
import io.github.e4c5.sqool.ast.SelectItem;
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.SourceSpan;
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
import java.util.regex.Pattern;
import org.antlr.v4.runtime.Token;

final class MysqlAstMapper {
  private static final Pattern SUPPORTED_IDENTIFIER =
      Pattern.compile("(?i)(`[^`]+`|[a-z_][a-z0-9_$]*)(\\.(?:`[^`]+`|[a-z_][a-z0-9_$]*)){0,2}");

  private MysqlAstMapper() {}

  static ParseResult mapQuerySpecification(
      MySQLParser.QuerySpecificationContext context, ParseOptions options) {
    if (context.fromClause() == null || context.fromClause().tableReferenceList() == null) {
      return unsupported("MySQL MVP currently requires a FROM clause.", context.start);
    }
    if (context.whereClause() != null
        || context.groupByClause() != null
        || context.havingClause() != null
        || context.windowClause() != null
        || context.qualifyClause() != null
        || context.intoClause() != null
        || !context.selectOption().isEmpty()) {
      return unsupported(
          "MySQL MVP currently supports only simple SELECT ... FROM statements.", context.start);
    }

    var tableReferences = context.fromClause().tableReferenceList().tableReference();
    if (tableReferences.size() != 1) {
      return unsupported(
          "MySQL MVP currently supports exactly one table reference.", context.start);
    }

    var table = mapTableReference(tableReferences.getFirst(), options);
    if (table == null) {
      return unsupported(
          "MySQL MVP currently supports only simple named table references.",
          tableReferences.getFirst().start);
    }

    var selectItems = mapSelectItems(context.selectItemList(), options);
    if (selectItems == null) {
      return unsupported(
          "MySQL MVP currently supports only '*' or identifier select items without aliases.",
          context.selectItemList().start);
    }

    return new ParseSuccess(
        SqlDialect.MYSQL,
        new SelectStatement(selectItems, table, span(context.start, context.stop, options)),
        List.of());
  }

  private static List<SelectItem> mapSelectItems(
      MySQLParser.SelectItemListContext context, ParseOptions options) {
    var result = new ArrayList<SelectItem>();
    if (context.MULT_OPERATOR() != null) {
      result.add(
          new AllColumnsSelectItem(
              span(
                  context.MULT_OPERATOR().getSymbol(),
                  context.MULT_OPERATOR().getSymbol(),
                  options)));
    }

    for (var selectItem : context.selectItem()) {
      if (selectItem.tableWild() != null
          || selectItem.selectAlias() != null
          || selectItem.expr() == null) {
        return null;
      }

      var expressionText = selectItem.expr().getText();
      if (!SUPPORTED_IDENTIFIER.matcher(expressionText).matches()) {
        return null;
      }

      result.add(
          new ExpressionSelectItem(
              new IdentifierExpression(
                  expressionText, span(selectItem.expr().start, selectItem.expr().stop, options)),
              span(selectItem.start, selectItem.stop, options)));
    }

    return List.copyOf(result);
  }

  private static NamedTableReference mapTableReference(
      MySQLParser.TableReferenceContext context, ParseOptions options) {
    if (!context.joinedTable().isEmpty() || context.tableFactor() == null) {
      return null;
    }

    var tableFactor = context.tableFactor();
    if (tableFactor.singleTable() == null) {
      return null;
    }

    var singleTable = tableFactor.singleTable();
    if (singleTable.usePartition() != null
        || singleTable.tableAlias() != null
        || singleTable.indexHintList() != null
        || singleTable.tablesampleClause() != null
        || singleTable.tableRef() == null) {
      return null;
    }

    var tableName = singleTable.tableRef().getText();
    if (!SUPPORTED_IDENTIFIER.matcher(tableName).matches()) {
      return null;
    }

    return new NamedTableReference(tableName, span(singleTable.start, singleTable.stop, options));
  }

  private static ParseFailure unsupported(String message, Token token) {
    return new ParseFailure(
        SqlDialect.MYSQL,
        List.of(
            new SyntaxDiagnostic(
                DiagnosticSeverity.ERROR,
                message,
                token == null ? 1 : token.getLine(),
                token == null ? 0 : token.getCharPositionInLine(),
                token == null ? null : token.getText())));
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
}
