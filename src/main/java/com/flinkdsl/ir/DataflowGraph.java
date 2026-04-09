package com.flinkdsl.ir;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The dataflow graph for a single pipeline.
 *
 * Nodes are stored in topological order (source first, sink last),
 * so codegen can iterate them in order without a separate sort.
 */
public class DataflowGraph {

    private final String pipelineName;
    private final int parallelism;
    private final List<DataflowNode> nodes;   // topological order
    private final List<DataflowEdge> edges;

    // Adjacency index built once for O(1) successor lookups
    private final Map<String, List<DataflowNode>> successorIndex;

    public DataflowGraph(String pipelineName, int parallelism,
                         List<DataflowNode> nodes, List<DataflowEdge> edges) {
        this.pipelineName = pipelineName;
        this.parallelism  = parallelism;
        this.nodes        = List.copyOf(nodes);
        this.edges        = List.copyOf(edges);

        // Build id → node lookup first
        Map<String, DataflowNode> byId = nodes.stream()
                .collect(Collectors.toMap(DataflowNode::id, n -> n));

        this.successorIndex = edges.stream()
                .collect(Collectors.groupingBy(
                        DataflowEdge::fromId,
                        Collectors.mapping(e -> byId.get(e.toId()), Collectors.toList())
                ));
    }

    public String pipelineName()  { return pipelineName; }
    public int    parallelism()   { return parallelism;  }

    /** All nodes in topological order. */
    public List<DataflowNode> nodes() { return nodes; }

    /** All directed edges. */
    public List<DataflowEdge> edges() { return edges; }

    /** Direct successors of the given node. */
    public List<DataflowNode> successors(DataflowNode node) {
        return successorIndex.getOrDefault(node.id(), List.of());
    }

    /** The single source node (always first). */
    public DataflowNode.Source source() {
        return (DataflowNode.Source) nodes.getFirst();
    }

    /** The single sink node (always last). */
    public DataflowNode.Sink sink() {
        return (DataflowNode.Sink) nodes.getLast();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataflowGraph[").append(pipelineName)
          .append(", parallelism=").append(parallelism).append("]\n");
        for (DataflowEdge e : edges) {
            sb.append("  ").append(e.fromId()).append(" -> ").append(e.toId()).append("\n");
        }
        return sb.toString();
    }
}
