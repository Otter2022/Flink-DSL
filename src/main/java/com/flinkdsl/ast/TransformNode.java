package com.flinkdsl.ast;

import java.util.List;

/**
 * Sealed hierarchy for the three transform forms in the grammar:
 *
 *   transformStmt
 *     : FILTER expression                       -> FilterTransform
 *     | MAP    LBRACE fieldAssignment+ RBRACE   -> MapTransform
 *     | FLATMAP LBRACE fieldAssignment+ RBRACE  -> FlatMapTransform
 */
public sealed interface TransformNode
        permits TransformNode.FilterTransform,
                TransformNode.MapTransform,
                TransformNode.FlatMapTransform {

    record FilterTransform(ExprNode predicate)              implements TransformNode {}
    record MapTransform(List<FieldAssignment> assignments)  implements TransformNode {}
    record FlatMapTransform(List<FieldAssignment> assignments) implements TransformNode {}
}
