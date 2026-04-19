package com.flinkdsl.analysis;

import com.flinkdsl.ast.*;

import java.util.*;

public class SemanticAnalyzer implements AstVisitor<Void> {

    private final List<SemanticError> errors = new ArrayList<>();

    private String currentPipeline             = null;
    private String currentSource               = null;
    private Map<String, SchemaType> currentSchema = Map.of();

    public List<SemanticError> analyze(Program program) {
        visitProgram(program);
        return Collections.unmodifiableList(errors);
    }

    @Override
    public Void visitProgram(Program node) {
        Set<String> seen = new HashSet<>();
        for (Pipeline p : node.pipelines()) {
            if (!seen.add(p.name())) {
                error(p.name(), "duplicate pipeline name '" + p.name() + "'");
            }
        }
        node.pipelines().forEach(this::visitPipeline);
        return null;
    }

    @Override
    public Void visitPipeline(Pipeline node) {
        currentPipeline = node.name();

        if (node.parallelism() < 1) {
            error("parallelism must be >= 1, got " + node.parallelism());
        }

        visitSourceNode(node.source());
        node.transforms().forEach(this::visitTransform);
        visitSinkNode(node.sink());

        currentPipeline = null;
        currentSource   = null;
        currentSchema   = Map.of();
        return null;
    }

    @Override
    public Void visitSourceNode(SourceNode node) {
        currentSource = node.name();

        Set<String> seen = new HashSet<>();
        for (FieldDecl f : node.schema()) {
            if (!seen.add(f.name())) {
                error("duplicate field '" + f.name() + "' in schema");
            }
        }

        Map<String, SchemaType> schema = new LinkedHashMap<>();
        for (FieldDecl f : node.schema()) {
            schema.put(f.name(), f.type());
        }
        currentSchema = Collections.unmodifiableMap(schema);
        return null;
    }

    @Override
    public Void visitSinkNode(SinkNode node) {
        return null;
    }

    @Override
    public Void visitFilterTransform(TransformNode.FilterTransform node) {
        SchemaType type = inferType(node.predicate());
        if (type != null && type != SchemaType.BOOLEAN) {
            error("filter predicate must be boolean, got " + type);
        }
        return null;
    }

    @Override
    public Void visitMapTransform(TransformNode.MapTransform node) {
        node.assignments().forEach(this::visitFieldAssignment);
        return null;
    }

    @Override
    public Void visitFlatMapTransform(TransformNode.FlatMapTransform node) {
        node.assignments().forEach(this::visitFieldAssignment);
        return null;
    }

    @Override
    public Void visitFieldAssignment(FieldAssignment node) {
        inferType(node.value());
        return null;
    }

    @Override public Void visitAndExpr(ExprNode.AndExpr node)           { inferType(node); return null; }
    @Override public Void visitOrExpr(ExprNode.OrExpr node)             { inferType(node); return null; }
    @Override public Void visitNotExpr(ExprNode.NotExpr node)           { inferType(node); return null; }
    @Override public Void visitCompareExpr(ExprNode.CompareExpr node)   { inferType(node); return null; }
    @Override public Void visitAddExpr(ExprNode.AddExpr node)           { inferType(node); return null; }
    @Override public Void visitMulExpr(ExprNode.MulExpr node)           { inferType(node); return null; }
    @Override public Void visitFieldAccessExpr(ExprNode.FieldAccessExpr node) { inferType(node); return null; }
    @Override public Void visitIntLiteral(ExprNode.IntLiteral node)     { return null; }
    @Override public Void visitFloatLiteral(ExprNode.FloatLiteral node) { return null; }
    @Override public Void visitStringLiteral(ExprNode.StringLiteral node) { return null; }
    @Override public Void visitBoolLiteral(ExprNode.BoolLiteral node)   { return null; }

    private SchemaType inferType(ExprNode expr) {
        return switch (expr) {
            case ExprNode.IntLiteral    ignored -> SchemaType.INT;
            case ExprNode.FloatLiteral  ignored -> SchemaType.DOUBLE;
            case ExprNode.StringLiteral ignored -> SchemaType.STRING;
            case ExprNode.BoolLiteral   ignored -> SchemaType.BOOLEAN;

            case ExprNode.FieldAccessExpr e -> {
                String root = e.parts().getFirst();
                if (!root.equals(currentSource)) {
                    error("unknown stream identifier '" + root
                            + "' — expected '" + currentSource + "'");
                    yield null;
                }
                if (e.parts().size() < 2) {
                    error("field access must be '" + currentSource + ".<fieldName>'");
                    yield null;
                }
                String fieldName = e.parts().get(1);
                SchemaType type  = currentSchema.get(fieldName);
                if (type == null) {
                    error("unknown field '" + fieldName + "' — not declared in schema");
                }
                yield type;
            }

            case ExprNode.AddExpr e -> {
                SchemaType l = inferType(e.left());
                SchemaType r = inferType(e.right());
                yield inferArithType(l, r, "'+'/'-'");
            }

            case ExprNode.MulExpr e -> {
                SchemaType l = inferType(e.left());
                SchemaType r = inferType(e.right());
                yield inferArithType(l, r, "'*'/'/'");
            }

            case ExprNode.CompareExpr e -> {
                SchemaType l = inferType(e.left());
                SchemaType r = inferType(e.right());
                if (l != null && r != null) {
                    checkComparable(l, r, e.op());
                }
                yield SchemaType.BOOLEAN;
            }

            case ExprNode.AndExpr e -> {
                SchemaType l = inferType(e.left());
                SchemaType r = inferType(e.right());
                if (l != null && l != SchemaType.BOOLEAN) error("'and' requires boolean operands, got " + l);
                if (r != null && r != SchemaType.BOOLEAN) error("'and' requires boolean operands, got " + r);
                yield SchemaType.BOOLEAN;
            }

            case ExprNode.OrExpr e -> {
                SchemaType l = inferType(e.left());
                SchemaType r = inferType(e.right());
                if (l != null && l != SchemaType.BOOLEAN) error("'or' requires boolean operands, got " + l);
                if (r != null && r != SchemaType.BOOLEAN) error("'or' requires boolean operands, got " + r);
                yield SchemaType.BOOLEAN;
            }

            case ExprNode.NotExpr e -> {
                SchemaType operand = inferType(e.operand());
                if (operand != null && operand != SchemaType.BOOLEAN) {
                    error("'not' requires a boolean operand, got " + operand);
                }
                yield SchemaType.BOOLEAN;
            }
        };
    }

    private SchemaType inferArithType(SchemaType l, SchemaType r, String op) {
        if (l == null || r == null) return null;
        if (!l.isNumeric()) { error(op + " requires numeric operands, got " + l); return null; }
        if (!r.isNumeric()) { error(op + " requires numeric operands, got " + r); return null; }
        return promoteNumeric(l, r);
    }

    private static SchemaType promoteNumeric(SchemaType a, SchemaType b) {
        if (a == SchemaType.DOUBLE || b == SchemaType.DOUBLE) return SchemaType.DOUBLE;
        if (a == SchemaType.FLOAT  || b == SchemaType.FLOAT)  return SchemaType.FLOAT;
        if (a == SchemaType.LONG   || b == SchemaType.LONG)   return SchemaType.LONG;
        return SchemaType.INT;
    }

    private void checkComparable(SchemaType l, SchemaType r, CompOp op) {
        boolean bothNumeric = l.isNumeric() && r.isNumeric();
        boolean sameType    = l == r;

        if (!bothNumeric && !sameType) {
            error("cannot compare " + l + " with " + r);
            return;
        }
        if (l == SchemaType.BOOLEAN && (op == CompOp.LT || op == CompOp.GT
                || op == CompOp.LTE || op == CompOp.GTE)) {
            error("operator " + op + " cannot be applied to boolean");
        }
    }

    private void error(String msg) {
        errors.add(new SemanticError("[" + currentPipeline + "] " + msg));
    }

    private void error(String pipeline, String msg) {
        errors.add(new SemanticError("[" + pipeline + "] " + msg));
    }
}
