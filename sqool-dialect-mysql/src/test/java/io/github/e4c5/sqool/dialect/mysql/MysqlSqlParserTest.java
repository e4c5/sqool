package io.github.e4c5.sqool.dialect.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.e4c5.sqool.ast.AllColumnsSelectItem;
import io.github.e4c5.sqool.ast.BetweenExpression;
import io.github.e4c5.sqool.ast.BinaryExpression;
import io.github.e4c5.sqool.ast.BinaryOperator;
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
import io.github.e4c5.sqool.ast.SelectStatement;
import io.github.e4c5.sqool.ast.SortDirection;
import io.github.e4c5.sqool.core.ParseFailure;
import io.github.e4c5.sqool.core.ParseOptions;
import io.github.e4c5.sqool.core.ParseSuccess;
import io.github.e4c5.sqool.core.SqlDialect;
import org.junit.jupiter.api.Test;

class MysqlSqlParserTest {
  private final MysqlSqlParser parser = new MysqlSqlParser();

  @Test
  void parsesSimpleSelectIntoAst() {
    var result = parser.parse("select id, name from demo", ParseOptions.defaults(SqlDialect.MYSQL));

    var success = assertInstanceOf(ParseSuccess.class, result);
    var statement = assertInstanceOf(SelectStatement.class, success.root());

    assertFalse(statement.distinct());
    assertEquals(2, statement.selectItems().size());
    var from = assertInstanceOf(NamedTableReference.class, statement.from());
    assertEquals("demo", from.name());
    assertNull(from.alias());
    assertNull(statement.where());
    assertTrue(statement.groupBy().isEmpty());
    assertNull(statement.having());
    assertTrue(statement.orderBy().isEmpty());
    assertNull(statement.limit());
    assertEquals(
        "id",
        assertInstanceOf(
                IdentifierExpression.class,
                assertInstanceOf(ExpressionSelectItem.class, statement.selectItems().get(0))
                    .expression())
            .text());
  }

  @Test
  void parsesAllColumnsProjection() {
    var result =
        parser.parse(
            "SELECT u.* FROM demo u ORDER BY u.id LIMIT 5",
            ParseOptions.defaults(SqlDialect.MYSQL));

    var success = assertInstanceOf(ParseSuccess.class, result);
    var statement = assertInstanceOf(SelectStatement.class, success.root());

    assertEquals(1, statement.selectItems().size());
    assertEquals(
        "u",
        assertInstanceOf(AllColumnsSelectItem.class, statement.selectItems().get(0)).qualifier());
    assertEquals(1, statement.orderBy().size());
    assertEquals(
        SortDirection.ASC,
        assertInstanceOf(OrderByItem.class, statement.orderBy().getFirst()).direction());
    assertEquals(5L, assertInstanceOf(LimitClause.class, statement.limit()).rowCount());
  }

  @Test
  void parsesAliasesWhereJoinOrderAndLimit() {
    var result =
        parser.parse(
            "select u.id as user_id, u.name name from users u "
                + "inner join orders o on u.id = o.user_id "
                + "where o.total >= 100 order by o.created_at desc limit 10 offset 5",
            ParseOptions.defaults(SqlDialect.MYSQL));

    var success = assertInstanceOf(ParseSuccess.class, result);
    var statement = assertInstanceOf(SelectStatement.class, success.root());
    var join = assertInstanceOf(JoinTableReference.class, statement.from());

    assertEquals(JoinType.INNER, join.joinType());
    assertEquals("users", assertInstanceOf(NamedTableReference.class, join.left()).name());
    assertEquals("u", assertInstanceOf(NamedTableReference.class, join.left()).alias());
    assertEquals("orders", assertInstanceOf(NamedTableReference.class, join.right()).name());
    assertEquals("o", assertInstanceOf(NamedTableReference.class, join.right()).alias());

    var joinCondition = assertInstanceOf(BinaryExpression.class, join.condition());
    assertEquals(BinaryOperator.EQUAL, joinCondition.operator());
    assertEquals("u.id", assertInstanceOf(IdentifierExpression.class, joinCondition.left()).text());
    assertEquals(
        "o.user_id", assertInstanceOf(IdentifierExpression.class, joinCondition.right()).text());

    var where = assertInstanceOf(BinaryExpression.class, statement.where());
    assertEquals(BinaryOperator.GREATER_OR_EQUAL, where.operator());
    assertEquals("o.total", assertInstanceOf(IdentifierExpression.class, where.left()).text());
    assertEquals("100", assertInstanceOf(LiteralExpression.class, where.right()).text());

    assertEquals(
        "user_id",
        assertInstanceOf(ExpressionSelectItem.class, statement.selectItems().get(0)).alias());
    assertEquals(
        "name",
        assertInstanceOf(ExpressionSelectItem.class, statement.selectItems().get(1)).alias());

    assertEquals(1, statement.orderBy().size());
    var orderBy = statement.orderBy().getFirst();
    assertEquals(SortDirection.DESC, orderBy.direction());
    assertEquals(
        "o.created_at", assertInstanceOf(IdentifierExpression.class, orderBy.expression()).text());

    var limit = assertInstanceOf(LimitClause.class, statement.limit());
    assertEquals(10L, limit.rowCount());
    assertEquals(5L, limit.offset());
  }

  @Test
  void parsesDistinctGroupedAggregates() {
    var result =
        parser.parse(
            "select distinct category, count(*) as total "
                + "from sales where amount between 10 and 20 "
                + "group by category having count(*) > 1 "
                + "order by total desc limit 3",
            ParseOptions.defaults(SqlDialect.MYSQL));

    var success = assertInstanceOf(ParseSuccess.class, result);
    var statement = assertInstanceOf(SelectStatement.class, success.root());

    assertTrue(statement.distinct());
    assertEquals(2, statement.selectItems().size());
    assertEquals(1, statement.groupBy().size());
    assertEquals(
        "category",
        assertInstanceOf(IdentifierExpression.class, statement.groupBy().getFirst()).text());
    var where = assertInstanceOf(BetweenExpression.class, statement.where());
    assertEquals("amount", assertInstanceOf(IdentifierExpression.class, where.expression()).text());
    assertEquals("10", assertInstanceOf(LiteralExpression.class, where.lowerBound()).text());
    assertEquals("20", assertInstanceOf(LiteralExpression.class, where.upperBound()).text());
    var aggregate =
        assertInstanceOf(
            FunctionCallExpression.class,
            assertInstanceOf(ExpressionSelectItem.class, statement.selectItems().get(1))
                .expression());
    assertEquals("COUNT", aggregate.name().toUpperCase());
    assertTrue(aggregate.starArgument());
    var having = assertInstanceOf(BinaryExpression.class, statement.having());
    assertEquals(BinaryOperator.GREATER_THAN, having.operator());
    assertInstanceOf(FunctionCallExpression.class, having.left());
    assertEquals("1", assertInstanceOf(LiteralExpression.class, having.right()).text());
  }

  @Test
  void parsesArithmeticInLikeAndFunctionCalls() {
    var result =
        parser.parse(
            "select price + tax as total, custom_score(id) as score "
                + "from orders where status in ('PAID', 'SHIPPED') "
                + "and customer_name like 'A%'",
            ParseOptions.defaults(SqlDialect.MYSQL));

    var success = assertInstanceOf(ParseSuccess.class, result);
    var statement = assertInstanceOf(SelectStatement.class, success.root());

    var total = assertInstanceOf(ExpressionSelectItem.class, statement.selectItems().get(0));
    assertEquals("total", total.alias());
    var totalExpr = assertInstanceOf(BinaryExpression.class, total.expression());
    assertEquals(BinaryOperator.PLUS, totalExpr.operator());

    var score = assertInstanceOf(ExpressionSelectItem.class, statement.selectItems().get(1));
    var function = assertInstanceOf(FunctionCallExpression.class, score.expression());
    assertEquals("custom_score", function.name());
    assertEquals(1, function.arguments().size());

    var where = assertInstanceOf(BinaryExpression.class, statement.where());
    assertEquals(BinaryOperator.AND, where.operator());
    var inExpression = assertInstanceOf(InExpression.class, where.left());
    assertEquals(
        "status", assertInstanceOf(IdentifierExpression.class, inExpression.expression()).text());
    assertEquals(2, inExpression.values().size());
    var likeExpression = assertInstanceOf(LikeExpression.class, where.right());
    assertEquals(
        "customer_name",
        assertInstanceOf(IdentifierExpression.class, likeExpression.expression()).text());
  }

  @Test
  void reportsUnsupportedSelectShapes() {
    var result =
        parser.parse("select coalesce(id, 0) from demo", ParseOptions.defaults(SqlDialect.MYSQL));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertFalse(failure.diagnostics().isEmpty());
    assertTrue(failure.diagnostics().getFirst().message().contains("runtime function"));
  }

  @Test
  void parsesInPredicates() {
    var result =
        parser.parse(
            "select id from demo where id in (1, 2)", ParseOptions.defaults(SqlDialect.MYSQL));

    var success = assertInstanceOf(ParseSuccess.class, result);
    var statement = assertInstanceOf(SelectStatement.class, success.root());
    assertInstanceOf(InExpression.class, statement.where());
  }

  @Test
  void reportsUnsupportedRegexPredicates() {
    var result =
        parser.parse(
            "select id from demo where customer_name regexp '^A'",
            ParseOptions.defaults(SqlDialect.MYSQL));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertFalse(failure.diagnostics().isEmpty());
    assertTrue(failure.diagnostics().getFirst().message().contains("predicate operation"));
  }

  @Test
  void reportsSyntaxErrors() {
    var result = parser.parse("select from demo", ParseOptions.defaults(SqlDialect.MYSQL));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertFalse(failure.diagnostics().isEmpty());
  }

  @Test
  void rejectsScriptModeForNow() {
    var result =
        parser.parse(
            "select id from demo; select name from other;",
            ParseOptions.defaults(SqlDialect.MYSQL).withScriptMode(true));

    var failure = assertInstanceOf(ParseFailure.class, result);
    assertEquals(
        "MySQL MVP does not support script mode yet.", failure.diagnostics().getFirst().message());
  }
}
