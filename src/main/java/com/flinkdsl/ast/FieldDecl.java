package com.flinkdsl.ast;

/** One field declaration inside a schema block: identifier : schemaType */
public record FieldDecl(String name, SchemaType type) {}
