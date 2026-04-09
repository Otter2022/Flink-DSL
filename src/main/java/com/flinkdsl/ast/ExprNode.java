package com.flinkdsl.ast;

import java.util.List;

/**
 * Sealed hierarchy covering every expression form in the grammar.
 *
 *   expression
 *     : expression AND expression          -> AndExpr
 *     | expression OR  expression          -> OrExpr
 *     | NOT expression                     -> NotExpr
 *     | expression compOp expression       -> CompareExpr
 *     | expression addOp  expression       -> AddExpr
 *     | expression mulOp  expression       -> MulExpr
 *     | LPAREN expression RPAREN           -> (transparent, no node)
 *     | fieldAccess                        -> FieldAccessExpr
 *     | literal                            -> IntLiteral | FloatLiteral
 *                                             | StringLiteral | BoolLiteral
 */
public sealed interface ExprNode
        permits ExprNode.AndExpr, ExprNode.OrExpr, ExprNode.NotExpr,
                ExprNode.CompareExpr, ExprNode.AddExpr, ExprNode.MulExpr,
                ExprNode.FieldAccessExpr,
                ExprNode.IntLiteral, ExprNode.FloatLiteral,
                ExprNode.StringLiteral, ExprNode.BoolLiteral {

    record AndExpr(ExprNode left, ExprNode right)               implements ExprNode {}
    record OrExpr(ExprNode left, ExprNode right)                implements ExprNode {}
    record NotExpr(ExprNode operand)                            implements ExprNode {}
    record CompareExpr(ExprNode left, CompOp op, ExprNode right) implements ExprNode {}
    record AddExpr(ExprNode left, AddOp op, ExprNode right)     implements ExprNode {}
    record MulExpr(ExprNode left, MulOp op, ExprNode right)     implements ExprNode {}

    /** Represents field.subfield.… access chains */
    record FieldAccessExpr(List<String> parts)                  implements ExprNode {}

    record IntLiteral(int value)                                implements ExprNode {}
    record FloatLiteral(double value)                           implements ExprNode {}
    record StringLiteral(String value)                          implements ExprNode {}
    record BoolLiteral(boolean value)                           implements ExprNode {}
}
