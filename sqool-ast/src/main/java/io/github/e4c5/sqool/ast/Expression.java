package io.github.e4c5.sqool.ast;

/** Marker interface for supported SQL expressions. */
public sealed interface Expression extends AstNode
    permits BetweenExpression,
        BinaryExpression,
        FunctionCallExpression,
        IdentifierExpression,
        InExpression,
        LikeExpression,
        LiteralExpression,
        UnaryExpression {}
