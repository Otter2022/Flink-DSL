package com.flinkdsl.ast;

public enum SchemaType {
    STRING, INT, LONG, DOUBLE, FLOAT, BOOLEAN;

    public boolean isNumeric() {
        return this == INT || this == LONG || this == DOUBLE || this == FLOAT;
    }
}
