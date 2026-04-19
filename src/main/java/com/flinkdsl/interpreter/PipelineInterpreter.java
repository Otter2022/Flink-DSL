package com.flinkdsl.interpreter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flinkdsl.ast.FieldAssignment;
import com.flinkdsl.ast.FieldDecl;
import com.flinkdsl.ast.SchemaType;
import com.flinkdsl.ir.DataflowGraph;
import com.flinkdsl.ir.DataflowNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PipelineInterpreter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DataflowGraph  graph;
    private final ExprEvaluator  evaluator;

    public PipelineInterpreter(DataflowGraph graph) {
        this.graph = graph;

        Map<String, SchemaType> schema = new LinkedHashMap<>();
        for (FieldDecl f : graph.source().schema()) {
            schema.put(f.name(), f.type());
        }
        this.evaluator = new ExprEvaluator(Collections.unmodifiableMap(schema));
    }

    public void run(Path inputFile, Path outputFile) throws IOException {
        List<ObjectNode> records = readInput(inputFile);

        for (DataflowNode node : graph.nodes()) {
            records = applyNode(node, records);
        }

        writeOutput(records, outputFile);
        System.out.printf("Interpreter [%s]: %d record(s) → %s%n",
                graph.pipelineName(), records.size(), outputFile);
    }

    private List<ObjectNode> applyNode(DataflowNode node, List<ObjectNode> records) {
        return switch (node) {
            case DataflowNode.Source  ignored -> records;
            case DataflowNode.Sink    ignored -> records;

            case DataflowNode.Filter f -> {
                List<ObjectNode> out = new ArrayList<>();
                for (ObjectNode r : records) {
                    if ((Boolean) evaluator.evaluate(f.predicate(), r)) {
                        out.add(r);
                    }
                }
                yield out;
            }

            case DataflowNode.Map m -> {
                List<ObjectNode> out = new ArrayList<>();
                for (ObjectNode r : records) {
                    out.add(applyAssignments(m.assignments(), r));
                }
                yield out;
            }

            case DataflowNode.FlatMap fm -> {
                List<ObjectNode> out = new ArrayList<>();
                for (ObjectNode r : records) {
                    out.add(applyAssignments(fm.assignments(), r));
                }
                yield out;
            }
        };
    }

    private ObjectNode applyAssignments(List<FieldAssignment> assignments, ObjectNode record) {
        ObjectNode out = MAPPER.createObjectNode();
        for (FieldAssignment a : assignments) {
            Object value = evaluator.evaluate(a.value(), record);
            putValue(out, a.field(), value);
        }
        return out;
    }

    private static void putValue(ObjectNode node, String field, Object value) {
        switch (value) {
            case String  v -> node.put(field, v);
            case Double  v -> node.put(field, v);
            case Float   v -> node.put(field, v);
            case Long    v -> node.put(field, v);
            case Integer v -> node.put(field, v);
            case Boolean v -> node.put(field, v);
            default        -> node.putPOJO(field, value);
        }
    }

    private List<ObjectNode> readInput(Path file) throws IOException {
        List<ObjectNode> records = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            if (!line.isBlank()) {
                records.add((ObjectNode) MAPPER.readTree(line));
            }
        }
        return records;
    }

    private void writeOutput(List<ObjectNode> records, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            for (ObjectNode record : records) {
                writer.write(MAPPER.writeValueAsString(record));
                writer.newLine();
            }
        }
    }
}
