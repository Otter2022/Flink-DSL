package com.flinkdsl.ir;

import com.flinkdsl.ast.ConnectorNode;
import com.flinkdsl.ast.ExprNode;
import com.flinkdsl.ast.FieldAssignment;
import com.flinkdsl.ast.FieldDecl;
import com.flinkdsl.ast.FormatType;

import java.util.List;

/**
 * A node in the dataflow graph. Each node corresponds to one operation
 * in the pipeline and carries exactly the AST data that codegen needs.
 *
 * The graph is always a linear chain for our grammar:
 *   Source → (Filter | Map | FlatMap)* → Sink
 */
public sealed interface DataflowNode
        permits DataflowNode.Source, DataflowNode.Filter,
                DataflowNode.Map, DataflowNode.FlatMap, DataflowNode.Sink {

    /** Unique identifier within the graph, e.g. "source_events", "filter_0". */
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
