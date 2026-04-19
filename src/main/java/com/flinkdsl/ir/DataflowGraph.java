package com.flinkdsl.ir;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataflowGraph {

    private final String pipelineName;
    private final int parallelism;
    private final List<DataflowNode> nodes;
    private final List<DataflowEdge> edges;
    private final Map<String, List<DataflowNode>> successorIndex;

    public DataflowGraph(String pipelineName, int parallelism,
                         List<DataflowNode> nodes, List<DataflowEdge> edges) {
        this.pipelineName = pipelineName;
        this.parallelism  = parallelism;
        this.nodes        = List.copyOf(nodes);
        this.edges        = List.copyOf(edges);

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

    public List<DataflowNode> nodes() { return nodes; }
    public List<DataflowEdge> edges() { return edges; }

    public List<DataflowNode> successors(DataflowNode node) {
        return successorIndex.getOrDefault(node.id(), List.of());
    }

    public DataflowNode.Source source() {
        return (DataflowNode.Source) nodes.getFirst();
    }

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
