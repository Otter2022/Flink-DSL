package com.flinkdsl.ast;

import java.util.List;

public sealed interface TransformNode
        permits TransformNode.FilterTransform,
                TransformNode.MapTransform,
                TransformNode.FlatMapTransform {

    record FieldAssignment(String field, ExprNode value) {}

    record FilterTransform(ExprNode predicate)                 implements TransformNode {}
    record MapTransform(List<FieldAssignment> assignments)     implements TransformNode {}
    record FlatMapTransform(List<FieldAssignment> assignments) implements TransformNode {}
}
