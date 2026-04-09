package com.flinkdsl.codegen;

import com.flinkdsl.ast.*;

import java.util.Map;

/**
 * Translates an ExprNode into a Java expression string suitable for use
 * inside a generated Flink lambda. Records are accessed as ObjectNode
 * via Jackson, so field reads use the appropriate JsonNode accessor method.
 *
 * Example: ExprNode for `events.amount * 1.1`
 *   → "(record.get(\"amount\").asDouble() * 1.1)"
 */
public class ExprCodegen {

    private final Map<String, SchemaType> schema;

    public ExprCodegen(Map<String, SchemaType> schema) {
        this.schema = schema;
    }

    // ── Main entry ────────────────────────────────────────────────────────────

    public String generate(ExprNode expr) {
        return switch (expr) {
            case ExprNode.IntLiteral    e -> String.valueOf(e.value());
            case ExprNode.FloatLiteral  e -> e.value() + "d";
            case ExprNode.StringLiteral e -> "\"" + e.value().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
            case ExprNode.BoolLiteral   e -> String.valueOf(e.value());

            case ExprNode.FieldAccessExpr e -> {
                String field    = e.parts().get(1);
                SchemaType type = schema.getOrDefault(field, SchemaType.STRING);
                yield "record.get(\"" + field + "\")" + jacksonAccessor(type);
            }

            case ExprNode.AddExpr e ->
                "(" + generate(e.left()) + " " + addSym(e.op()) + " " + generate(e.right()) + ")";

            case ExprNode.MulExpr e ->
                "(" + generate(e.left()) + " " + mulSym(e.op()) + " " + generate(e.right()) + ")";

            case ExprNode.CompareExpr e -> compareCode(e);

            case ExprNode.AndExpr e ->
                "(" + generate(e.left()) + " && " + generate(e.right()) + ")";

            case ExprNode.OrExpr e ->
                "(" + generate(e.left()) + " || " + generate(e.right()) + ")";

            case ExprNode.NotExpr e ->
                "(!" + generate(e.operand()) + ")";
        };
    }

    // ── Type inference (used for comparison dispatch) ─────────────────────────

    /** Returns the Java type that generate() will produce for this expression. */
    SchemaType inferType(ExprNode expr) {
        return switch (expr) {
            case ExprNode.IntLiteral    ignored -> SchemaType.INT;
            case ExprNode.FloatLiteral  ignored -> SchemaType.DOUBLE;
            case ExprNode.StringLiteral ignored -> SchemaType.STRING;
            case ExprNode.BoolLiteral   ignored -> SchemaType.BOOLEAN;
            case ExprNode.FieldAccessExpr e     -> schema.getOrDefault(e.parts().get(1), SchemaType.STRING);
            case ExprNode.AddExpr e             -> promoteNumeric(inferType(e.left()), inferType(e.right()));
            case ExprNode.MulExpr e             -> promoteNumeric(inferType(e.left()), inferType(e.right()));
            default                             -> SchemaType.BOOLEAN;
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String compareCode(ExprNode.CompareExpr e) {
        String lc = generate(e.left());
        String rc = generate(e.right());

        // Strings need .equals() instead of == / !=
        if (inferType(e.left()) == SchemaType.STRING) {
            return switch (e.op()) {
                case EQ  -> lc + ".equals(" + rc + ")";
                case NEQ -> "!" + lc + ".equals(" + rc + ")";
                default  -> throw new IllegalStateException("Ordering comparison on STRING");
            };
        }

        String op = switch (e.op()) {
            case EQ  -> "=="; case NEQ -> "!=";
            case LT  -> "<";  case GT  -> ">";
            case LTE -> "<="; case GTE -> ">=";
        };
        return "(" + lc + " " + op + " " + rc + ")";
    }

    /** Jackson accessor suffix for reading a typed value from a JsonNode. */
    static String jacksonAccessor(SchemaType type) {
        return switch (type) {
            case STRING  -> ".asText()";
            case INT     -> ".asInt()";
            case LONG    -> ".asLong()";
            case DOUBLE  -> ".asDouble()";
            case FLOAT   -> ".floatValue()";
            case BOOLEAN -> ".asBoolean()";
        };
    }

    private static SchemaType promoteNumeric(SchemaType a, SchemaType b) {
        if (a == SchemaType.DOUBLE || b == SchemaType.DOUBLE) return SchemaType.DOUBLE;
        if (a == SchemaType.FLOAT  || b == SchemaType.FLOAT)  return SchemaType.FLOAT;
        if (a == SchemaType.LONG   || b == SchemaType.LONG)   return SchemaType.LONG;
        return SchemaType.INT;
    }

    private static String addSym(AddOp op) { return op == AddOp.PLUS  ? "+" : "-"; }
    private static String mulSym(MulOp op) { return op == MulOp.STAR  ? "*" : "/"; }
}
