package com.flinkdsl.ast;

/** One entry inside a map / flatMap block: identifier = expression */
public record FieldAssignment(String field, ExprNode value) {}
