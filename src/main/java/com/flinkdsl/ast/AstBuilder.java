package com.flinkdsl.ast;

import com.flinkdsl.grammar.FlinkPipelineBaseVisitor;
import com.flinkdsl.grammar.FlinkPipelineParser;

import java.util.List;
import java.util.stream.Collectors;

public class AstBuilder extends FlinkPipelineBaseVisitor<Object> {

    @Override
    public Program visitProgram(FlinkPipelineParser.ProgramContext ctx) {
        List<Pipeline> pipelines = ctx.pipeline().stream()
                .map(p -> (Pipeline) visitPipeline(p))
                .collect(Collectors.toList());
        return new Program(pipelines);
    }

    @Override
    public Pipeline visitPipeline(FlinkPipelineParser.PipelineContext ctx) {
        String name        = ctx.identifier().getText();
        int parallelism    = Integer.parseInt(ctx.parallelismStmt().INTEGER().getText());
        SourceNode source  = (SourceNode) visitSourceStmt(ctx.sourceStmt());
        List<TransformNode> transforms = ctx.transformStmt().stream()
                .map(t -> (TransformNode) visit(t))
                .collect(Collectors.toList());
        SinkNode sink      = (SinkNode) visitSinkStmt(ctx.sinkStmt());
        return new Pipeline(name, parallelism, source, transforms, sink);
    }

    @Override
    public SourceNode visitSourceStmt(FlinkPipelineParser.SourceStmtContext ctx) {
        String name             = ctx.identifier().getText();
        ConnectorNode connector = (ConnectorNode) visit(ctx.connectorExpr());
        List<FieldDecl> schema  = ctx.schemaBlock().fieldDecl().stream()
                .map(f -> (FieldDecl) visitFieldDecl(f))
                .collect(Collectors.toList());
        return new SourceNode(name, connector, schema);
    }

    @Override
    public FieldDecl visitFieldDecl(FlinkPipelineParser.FieldDeclContext ctx) {
        String name    = ctx.identifier().getText();
        SchemaType type = parseSchemaType(ctx.schemaType());
        return new FieldDecl(name, type);
    }

    private static SchemaType parseSchemaType(FlinkPipelineParser.SchemaTypeContext ctx) {
        if (ctx.T_STRING()  != null) return SchemaType.STRING;
        if (ctx.T_INT()     != null) return SchemaType.INT;
        if (ctx.T_LONG()    != null) return SchemaType.LONG;
        if (ctx.T_DOUBLE()  != null) return SchemaType.DOUBLE;
        if (ctx.T_FLOAT()   != null) return SchemaType.FLOAT;
        if (ctx.T_BOOLEAN() != null) return SchemaType.BOOLEAN;
        throw new IllegalStateException("Unknown schema type: " + ctx.getText());
    }

    @Override
    public ConnectorNode visitKafkaConnector(FlinkPipelineParser.KafkaConnectorContext ctx) {
        return new ConnectorNode.KafkaConnector(stripQuotes(ctx.stringLiteral().getText()));
    }

    @Override
    public ConnectorNode visitFileConnector(FlinkPipelineParser.FileConnectorContext ctx) {
        return new ConnectorNode.FileConnector(stripQuotes(ctx.stringLiteral().getText()));
    }

    @Override
    public TransformNode visitFilterTransform(FlinkPipelineParser.FilterTransformContext ctx) {
        return new TransformNode.FilterTransform((ExprNode) visit(ctx.expression()));
    }

    @Override
    public TransformNode visitMapTransform(FlinkPipelineParser.MapTransformContext ctx) {
        return new TransformNode.MapTransform(buildAssignments(ctx.fieldAssignment()));
    }

    @Override
    public TransformNode visitFlatMapTransform(FlinkPipelineParser.FlatMapTransformContext ctx) {
        return new TransformNode.FlatMapTransform(buildAssignments(ctx.fieldAssignment()));
    }

    @Override
    public FieldAssignment visitFieldAssignment(FlinkPipelineParser.FieldAssignmentContext ctx) {
        String field  = ctx.identifier().getText();
        ExprNode value = (ExprNode) visit(ctx.expression());
        return new FieldAssignment(field, value);
    }

    private List<FieldAssignment> buildAssignments(
            List<FlinkPipelineParser.FieldAssignmentContext> ctxs) {
        return ctxs.stream()
                .map(a -> (FieldAssignment) visitFieldAssignment(a))
                .collect(Collectors.toList());
    }

    @Override
    public SinkNode visitSinkStmt(FlinkPipelineParser.SinkStmtContext ctx) {
        ConnectorNode connector = (ConnectorNode) visit(ctx.connectorExpr());
        FormatType format = ctx.formatType().JSON() != null ? FormatType.JSON : FormatType.CSV;
        return new SinkNode(connector, format);
    }

    @Override
    public ExprNode visitAndExpr(FlinkPipelineParser.AndExprContext ctx) {
        return new ExprNode.AndExpr(
                (ExprNode) visit(ctx.expression(0)),
                (ExprNode) visit(ctx.expression(1)));
    }

    @Override
    public ExprNode visitOrExpr(FlinkPipelineParser.OrExprContext ctx) {
        return new ExprNode.OrExpr(
                (ExprNode) visit(ctx.expression(0)),
                (ExprNode) visit(ctx.expression(1)));
    }

    @Override
    public ExprNode visitNotExpr(FlinkPipelineParser.NotExprContext ctx) {
        return new ExprNode.NotExpr((ExprNode) visit(ctx.expression()));
    }

    @Override
    public ExprNode visitCompareExpr(FlinkPipelineParser.CompareExprContext ctx) {
        CompOp op = switch (ctx.compOp().getText()) {
            case "==" -> CompOp.EQ;
            case "!=" -> CompOp.NEQ;
            case "<"  -> CompOp.LT;
            case ">"  -> CompOp.GT;
            case "<=" -> CompOp.LTE;
            case ">=" -> CompOp.GTE;
            default   -> throw new IllegalStateException("Unknown compOp: " + ctx.compOp().getText());
        };
        return new ExprNode.CompareExpr(
                (ExprNode) visit(ctx.expression(0)),
                op,
                (ExprNode) visit(ctx.expression(1)));
    }

    @Override
    public ExprNode visitAddExpr(FlinkPipelineParser.AddExprContext ctx) {
        AddOp op = ctx.addOp().PLUS() != null ? AddOp.PLUS : AddOp.MINUS;
        return new ExprNode.AddExpr(
                (ExprNode) visit(ctx.expression(0)),
                op,
                (ExprNode) visit(ctx.expression(1)));
    }

    @Override
    public ExprNode visitMulExpr(FlinkPipelineParser.MulExprContext ctx) {
        MulOp op = ctx.mulOp().STAR() != null ? MulOp.STAR : MulOp.SLASH;
        return new ExprNode.MulExpr(
                (ExprNode) visit(ctx.expression(0)),
                op,
                (ExprNode) visit(ctx.expression(1)));
    }

    @Override
    public ExprNode visitParenExpr(FlinkPipelineParser.ParenExprContext ctx) {
        return (ExprNode) visit(ctx.expression());
    }

    @Override
    public ExprNode visitFieldExpr(FlinkPipelineParser.FieldExprContext ctx) {
        return (ExprNode) visitFieldAccess(ctx.fieldAccess());
    }

    @Override
    public ExprNode visitFieldAccess(FlinkPipelineParser.FieldAccessContext ctx) {
        List<String> parts = ctx.identifier().stream()
                .map(id -> id.getText())
                .collect(Collectors.toList());
        return new ExprNode.FieldAccessExpr(parts);
    }

    @Override
    public ExprNode visitLiteralExpr(FlinkPipelineParser.LiteralExprContext ctx) {
        return (ExprNode) visit(ctx.literal());
    }

    @Override
    public ExprNode visitIntLiteral(FlinkPipelineParser.IntLiteralContext ctx) {
        return new ExprNode.IntLiteral(Integer.parseInt(ctx.INTEGER().getText()));
    }

    @Override
    public ExprNode visitFloatLiteral(FlinkPipelineParser.FloatLiteralContext ctx) {
        return new ExprNode.FloatLiteral(Double.parseDouble(ctx.FLOAT().getText()));
    }

    @Override
    public ExprNode visitStrLiteral(FlinkPipelineParser.StrLiteralContext ctx) {
        return new ExprNode.StringLiteral(stripQuotes(ctx.stringLiteral().getText()));
    }

    @Override
    public ExprNode visitBoolLiteral(FlinkPipelineParser.BoolLiteralContext ctx) {
        return new ExprNode.BoolLiteral(Boolean.parseBoolean(ctx.BOOLEAN().getText()));
    }

    private static String stripQuotes(String text) {
        return text.substring(1, text.length() - 1).replace("\\\"", "\"");
    }
}
