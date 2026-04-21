package com.flinkdsl.interpreter;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flinkdsl.ast.*;
import static com.flinkdsl.ast.ExprNode.AddOp;
import static com.flinkdsl.ast.ExprNode.MulOp;
import static com.flinkdsl.ast.ExprNode.CompOp;

import java.util.Map;

public class ExprEvaluator {

    private final Map<String, SchemaType> schema;

    public ExprEvaluator(Map<String, SchemaType> schema) {
        this.schema = schema;
    }

    public Object evaluate(ExprNode expr, ObjectNode record) {
        return switch (expr) {
            case ExprNode.IntLiteral    e -> e.value();
            case ExprNode.FloatLiteral  e -> e.value();
            case ExprNode.StringLiteral e -> e.value();
            case ExprNode.BoolLiteral   e -> e.value();

            case ExprNode.FieldAccessExpr e -> {
                String     field = e.parts().get(1);
                SchemaType type  = schema.getOrDefault(field, SchemaType.STRING);
                var        node  = record.get(field);
                if (node == null || node.isNull()) yield defaultValue(type);
                yield switch (type) {
                    case STRING  -> node.asText();
                    case INT     -> node.asInt();
                    case LONG    -> node.asLong();
                    case DOUBLE  -> node.asDouble();
                    case FLOAT   -> node.floatValue();
                    case BOOLEAN -> node.asBoolean();
                };
            }

            case ExprNode.AddExpr e -> {
                Object l = evaluate(e.left(), record);
                Object r = evaluate(e.right(), record);
                yield e.op() == AddOp.PLUS ? add(l, r) : subtract(l, r);
            }

            case ExprNode.MulExpr e -> {
                Object l = evaluate(e.left(), record);
                Object r = evaluate(e.right(), record);
                yield e.op() == MulOp.STAR ? multiply(l, r) : divide(l, r);
            }

            case ExprNode.CompareExpr e -> {
                Object l = evaluate(e.left(), record);
                Object r = evaluate(e.right(), record);
                yield compare(l, r, e.op());
            }

            case ExprNode.AndExpr e ->
                (Boolean) evaluate(e.left(), record) && (Boolean) evaluate(e.right(), record);

            case ExprNode.OrExpr e ->
                (Boolean) evaluate(e.left(), record) || (Boolean) evaluate(e.right(), record);

            case ExprNode.NotExpr e ->
                !(Boolean) evaluate(e.operand(), record);
        };
    }

    private static Object add(Object l, Object r) {
        if (isDouble(l, r)) return toDouble(l) + toDouble(r);
        if (isFloat(l, r))  return toFloat(l)  + toFloat(r);
        if (isLong(l, r))   return toLong(l)   + toLong(r);
        return toInt(l) + toInt(r);
    }

    private static Object subtract(Object l, Object r) {
        if (isDouble(l, r)) return toDouble(l) - toDouble(r);
        if (isFloat(l, r))  return toFloat(l)  - toFloat(r);
        if (isLong(l, r))   return toLong(l)   - toLong(r);
        return toInt(l) - toInt(r);
    }

    private static Object multiply(Object l, Object r) {
        if (isDouble(l, r)) return toDouble(l) * toDouble(r);
        if (isFloat(l, r))  return toFloat(l)  * toFloat(r);
        if (isLong(l, r))   return toLong(l)   * toLong(r);
        return toInt(l) * toInt(r);
    }

    private static Object divide(Object l, Object r) {
        return toDouble(l) / toDouble(r);
    }

    private static boolean compare(Object l, Object r, CompOp op) {
        if (l instanceof String ls) {
            String rs = (String) r;
            return switch (op) {
                case EQ  ->  ls.equals(rs);
                case NEQ -> !ls.equals(rs);
                default  -> throw new IllegalStateException("Ordering comparison on STRING");
            };
        }
        double ld = toDouble(l), rd = toDouble(r);
        return switch (op) {
            case EQ  -> ld == rd;
            case NEQ -> ld != rd;
            case LT  -> ld <  rd;
            case GT  -> ld >  rd;
            case LTE -> ld <= rd;
            case GTE -> ld >= rd;
        };
    }

    private static boolean isDouble(Object l, Object r) {
        return l instanceof Double || r instanceof Double;
    }
    private static boolean isFloat(Object l, Object r) {
        return l instanceof Float  || r instanceof Float;
    }
    private static boolean isLong(Object l, Object r) {
        return l instanceof Long   || r instanceof Long;
    }

    private static double toDouble(Object v) { return ((Number) v).doubleValue(); }
    private static float  toFloat(Object v)  { return ((Number) v).floatValue();  }
    private static long   toLong(Object v)   { return ((Number) v).longValue();   }
    private static int    toInt(Object v)    { return ((Number) v).intValue();    }

    private static Object defaultValue(SchemaType type) {
        return switch (type) {
            case STRING  -> "";
            case INT     -> 0;
            case LONG    -> 0L;
            case DOUBLE  -> 0.0;
            case FLOAT   -> 0.0f;
            case BOOLEAN -> false;
        };
    }
}
