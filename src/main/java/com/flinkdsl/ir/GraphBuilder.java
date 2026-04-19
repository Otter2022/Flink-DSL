package com.flinkdsl.ir;

import com.flinkdsl.ast.*;

import java.util.ArrayList;
import java.util.List;

public class GraphBuilder {

    public List<DataflowGraph> build(Program program) {
        return program.pipelines().stream()
                .map(this::buildGraph)
                .toList();
    }

    private DataflowGraph buildGraph(Pipeline pipeline) {
        List<DataflowNode> nodes = new ArrayList<>();
        List<DataflowEdge> edges = new ArrayList<>();

        DataflowNode.Source source = new DataflowNode.Source(
                "source_" + pipeline.source().name(),
                pipeline.source().name(),
                pipeline.source().connector(),
                pipeline.source().schema()
        );
        nodes.add(source);

        DataflowNode prev = source;
        int filterIdx = 0, mapIdx = 0, flatMapIdx = 0;

        for (TransformNode transform : pipeline.transforms()) {
            DataflowNode node = switch (transform) {
                case TransformNode.FilterTransform t ->
                        new DataflowNode.Filter("filter_" + filterIdx++, t.predicate());
                case TransformNode.MapTransform t ->
                        new DataflowNode.Map("map_" + mapIdx++, t.assignments());
                case TransformNode.FlatMapTransform t ->
                        new DataflowNode.FlatMap("flatmap_" + flatMapIdx++, t.assignments());
            };
            nodes.add(node);
            edges.add(new DataflowEdge(prev.id(), node.id()));
            prev = node;
        }

        DataflowNode.Sink sink = new DataflowNode.Sink(
                "sink",
                pipeline.sink().connector(),
                pipeline.sink().format()
        );
        nodes.add(sink);
        edges.add(new DataflowEdge(prev.id(), sink.id()));

        return new DataflowGraph(pipeline.name(), pipeline.parallelism(), nodes, edges);
    }
}
