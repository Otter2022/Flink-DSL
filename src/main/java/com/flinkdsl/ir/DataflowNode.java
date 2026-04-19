package com.flinkdsl.ir;

import com.flinkdsl.ast.ConnectorNode;
import com.flinkdsl.ast.ExprNode;
import com.flinkdsl.ast.FieldAssignment;
import com.flinkdsl.ast.FieldDecl;
import com.flinkdsl.ast.FormatType;

import java.util.List;

public sealed interface DataflowNode
        permits DataflowNode.Source, DataflowNode.Filter,
                DataflowNode.Map, DataflowNode.FlatMap, DataflowNode.Sink {

    String id();

    record Source(
            String id,
            String streamName,
            ConnectorNode connector,
            List<FieldDecl> schema
    ) implements DataflowNode {}

    record Filter(
            String id,
            ExprNode predicate
    ) implements DataflowNode {}

    record Map(
            String id,
            List<FieldAssignment> assignments
    ) implements DataflowNode {}

    record FlatMap(
            String id,
            List<FieldAssignment> assignments
    ) implements DataflowNode {}

    record Sink(
            String id,
            ConnectorNode connector,
            FormatType format
    ) implements DataflowNode {}
}
