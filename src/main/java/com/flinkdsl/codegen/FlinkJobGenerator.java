package com.flinkdsl.codegen;

import com.flinkdsl.ir.DataflowGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes one generated Flink job Java source file per pipeline to an output directory.
 *
 * Output file naming: <PipelineName>Job.java  (e.g. "clickstream" → "ClickstreamJob.java")
 */
public class FlinkJobGenerator {

    public void generate(List<DataflowGraph> graphs, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        for (DataflowGraph graph : graphs) {
            String className  = PipelineCodegen.capitalize(graph.pipelineName()) + "Job";
            String source     = new PipelineCodegen(graph).generate();
            Path   outputFile = outputDir.resolve(className + ".java");
            Files.writeString(outputFile, source);
            System.out.println("Generated: " + outputFile.toAbsolutePath());
        }
    }
}
