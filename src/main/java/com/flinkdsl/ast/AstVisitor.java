package com.flinkdsl.ast;

import static com.flinkdsl.ast.TransformNode.FieldAssignment;

public interface AstVisitor<T> {

    T visitProgram(Program node);
    T visitPipeline(Pipeline node);
    T visitSourceNode(SourceNode node);
    T visitSinkNode(SinkNode node);

    T visitFilterTransform(TransformNode.FilterTransform node);
    T visitMapTransform(TransformNode.MapTransform node);
    T visitFlatMapTransform(TransformNode.FlatMapTransform node);
    T visitFieldAssignment(FieldAssignment node);

    T visitAndExpr(ExprNode.AndExpr node);
    T visitOrExpr(ExprNode.OrExpr node);
    T visitNotExpr(ExprNode.NotExpr node);
    T visitCompareExpr(ExprNode.CompareExpr node);
    T visitAddExpr(ExprNode.AddExpr node);
    T visitMulExpr(ExprNode.MulExpr node);
    T visitFieldAccessExpr(ExprNode.FieldAccessExpr node);
    T visitIntLiteral(ExprNode.IntLiteral node);
    T visitFloatLiteral(ExprNode.FloatLiteral node);
    T visitStringLiteral(ExprNode.StringLiteral node);
    T visitBoolLiteral(ExprNode.BoolLiteral node);

    default T visitExpr(ExprNode node) {
        return switch (node) {
            case ExprNode.AndExpr      e -> visitAndExpr(e);
            case ExprNode.OrExpr       e -> visitOrExpr(e);
            case ExprNode.NotExpr      e -> visitNotExpr(e);
            case ExprNode.CompareExpr  e -> visitCompareExpr(e);
            case ExprNode.AddExpr      e -> visitAddExpr(e);
            case ExprNode.MulExpr      e -> visitMulExpr(e);
            case ExprNode.FieldAccessExpr e -> visitFieldAccessExpr(e);
            case ExprNode.IntLiteral   e -> visitIntLiteral(e);
            case ExprNode.FloatLiteral e -> visitFloatLiteral(e);
            case ExprNode.StringLiteral e -> visitStringLiteral(e);
            case ExprNode.BoolLiteral  e -> visitBoolLiteral(e);
        };
    }

    default T visitTransform(TransformNode node) {
        return switch (node) {
            case TransformNode.FilterTransform  t -> visitFilterTransform(t);
            case TransformNode.MapTransform     t -> visitMapTransform(t);
            case TransformNode.FlatMapTransform t -> visitFlatMapTransform(t);
        };
    }
}
