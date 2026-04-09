package com.flinkdsl.ast;

import java.util.List;

/** source <name> from <connector> schema { <fields> } */
public record SourceNode(String name, ConnectorNode connector, List<FieldDecl> schema) {}
