package com.flinkdsl;

import com.flinkdsl.analysis.SemanticAnalyzer;
import com.flinkdsl.analysis.SemanticError;
import com.flinkdsl.ast.AstBuilder;
import com.flinkdsl.ast.Program;
import com.flinkdsl.codegen.FlinkJobGenerator;
import com.flinkdsl.grammar.FlinkPipelineLexer;
import com.flinkdsl.grammar.FlinkPipelineParser;
import com.flinkdsl.interpreter.PipelineInterpreter;
import com.flinkdsl.ir.DataflowGraph;
import com.flinkdsl.ir.GraphBuilder;
import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {

    private static final String USAGE = """
            Usage:
              flink-dsl <program.flink> [output-dir]          — generate Flink job code
              flink-dsl --run <program.flink> <input> <output> — run locally on files
            """;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) { System.err.print(USAGE); System.exit(1); }

        if (args[0].equals("--run")) {
            if (args.length != 4) { System.err.print(USAGE); System.exit(1); }
            runInterpreter(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
        } else {
            if (args.length > 2)  { System.err.print(USAGE); System.exit(1); }
            Path outputDir = args.length == 2 ? Path.of(args[1]) : Path.of("generated");
            runCodegen(Path.of(args[0]), outputDir);
        }
    }

    // ── Shared: parse → AST → semantic check → IR ─────────────────────────────

    private static List<DataflowGraph> compile(Path inputFile) throws IOException {
        CharStream input = CharStreams.fromPath(inputFile);

        FlinkPipelineLexer lexer = new FlinkPipelineLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener("Lex"));

        CommonTokenStream tokens = new CommonTokenStream(lexer);

        FlinkPipelineParser parser = new FlinkPipelineParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener("Parse"));

        FlinkPipelineParser.ProgramContext parseTree = parser.program();
        if (parser.getNumberOfSyntaxErrors() > 0) {
            System.err.println("Aborting: syntax errors.");
            System.exit(1);
        }

        Program ast = new AstBuilder().visitProgram(parseTree);

        List<SemanticError> errors = new SemanticAnalyzer().analyze(ast);
        if (!errors.isEmpty()) {
            errors.forEach(System.err::println);
            System.exit(1);
        }

        return new GraphBuilder().build(ast);
    }

    // ── Code generation mode ──────────────────────────────────────────────────

    private static void runCodegen(Path inputFile, Path outputDir) throws Exception {
        List<DataflowGraph> graphs = compile(inputFile);
        new FlinkJobGenerator().generate(graphs, outputDir);
    }

    // ── Local interpreter mode ────────────────────────────────────────────────

    private static void runInterpreter(Path dslFile, Path inputFile, Path outputFile)
            throws Exception {
        List<DataflowGraph> graphs = compile(dslFile);
        for (DataflowGraph graph : graphs) {
            new PipelineInterpreter(graph).run(inputFile, outputFile);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static BaseErrorListener errorListener(String phase) {
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                    int line, int charPositionInLine,
                                    String msg, RecognitionException e) {
                System.err.printf("[%s] %d:%d — %s%n", phase, line, charPositionInLine, msg);
            }
        };
    }
}
