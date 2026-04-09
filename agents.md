# Flink DSL — Project Agent Guide

A compiler that translates declarative stream-processing pipeline descriptions into
executable Apache Flink 2.2 DataStream API jobs.

---

## Compiler pipeline

```
DSL source file
    │
    ▼  ANTLR4 (FlinkPipeline.g4)
Lexer + Parser
    │
    ▼  AstBuilder  (com.flinkdsl.ast)
Typed AST
    │
    ▼  SemanticAnalyzer  (com.flinkdsl.analysis)
Validated AST
    │
    ▼  GraphBuilder  (com.flinkdsl.ir)
Dataflow Graph (IR)
    │
    ├──▶  FlinkJobGenerator  (com.flinkdsl.codegen)  →  <Pipeline>Job.java
    │
    └──▶  PipelineInterpreter  (com.flinkdsl.interpreter)  →  local file execution
```

---

## Build & run

```bash
mvn package                          # compiles grammar + sources, produces fat jar

# Generate a Flink job Java source file
java -jar target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
     <program.flink> [output-dir]

# Run the pipeline locally on JSON-lines files (no Flink cluster needed)
java -jar target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
     --run <program.flink> <input.jsonl> <output.jsonl>
```

---

## DSL syntax

```
pipeline <name> {
    parallelism <int>

    source <streamName> from <connector> schema {
        <fieldName> : <type>
        ...
    }

    filter <expression>          // zero or more, order preserved
    map    { <field> = <expr> }  // zero or more
    flatMap { <field> = <expr> } // zero or more

    sink to <connector> format <json|csv>
}
```

**Connectors:** `kafka("<topic>")` | `file("<path>")`

**Schema types:** `string` `int` `long` `double` `float` `boolean`

**Expressions:** field access (`stream.field`), arithmetic (`+ - * /`),
comparisons (`== != < > <= >=`), boolean logic (`and or not`),
integer/float/string/boolean literals.

A complete example lives at `examples/hello.flink`.
Sample input data lives at `examples/input.jsonl`.

---

## Package structure

| Package | Purpose |
|---|---|
| `com.flinkdsl` | `Main.java` — CLI entry point |
| `com.flinkdsl.ast` | Typed AST nodes (sealed interfaces + records) |
| `com.flinkdsl.analysis` | Semantic analysis and type checking |
| `com.flinkdsl.ir` | Dataflow graph IR (nodes, edges, graph) |
| `com.flinkdsl.codegen` | Flink DataStream API Java source generation |
| `com.flinkdsl.interpreter` | Local execution on JSON-lines files |

---

## Key files

| File | Role |
|---|---|
| `src/main/antlr4/com/flinkdsl/grammar/FlinkPipeline.g4` | Grammar — single source of truth for the language |
| `ast/AstBuilder.java` | ANTLR parse tree → typed AST |
| `ast/AstVisitor.java` | Visitor interface with `visitExpr()` / `visitTransform()` dispatch helpers |
| `ast/ExprNode.java` | Sealed hierarchy for all 11 expression forms |
| `ast/TransformNode.java` | Sealed hierarchy for filter / map / flatMap |
| `analysis/SemanticAnalyzer.java` | Structural checks + type inference on expressions |
| `ir/GraphBuilder.java` | AST → `DataflowGraph` (nodes in topological order) |
| `codegen/ExprCodegen.java` | `ExprNode` → Java expression string |
| `codegen/PipelineCodegen.java` | `DataflowGraph` → complete `<Name>Job.java` source |
| `interpreter/ExprEvaluator.java` | `ExprNode` → live Java value (for local execution) |
| `interpreter/PipelineInterpreter.java` | Executes a graph on `List<ObjectNode>` records |

---

## Semantic checks

1. No duplicate pipeline names in a program.
2. `parallelism` must be ≥ 1.
3. No duplicate field names within a schema block.
4. Every field access root must equal the source stream name.
5. Every field name must be declared in the schema.
6. `filter` predicates must be boolean-typed.
7. Arithmetic operands (`+ - * /`) must be numeric.
8. Comparison operands must be compatible types.
9. `and` / `or` operands must be boolean.
10. `not` operand must be boolean.

---

## Design decisions

- **Grammar is intentionally constrained.** Scope is limited to one source,
  sequential transforms, and one sink per pipeline. Resist adding features
  unless they are required by the compiler pipeline itself.

- **Java 25** is the compile target. Pattern matching in switch and sealed
  classes are used throughout — do not downgrade.

- **Sealed interfaces + records** are used for AST and IR nodes. This gives
  exhaustive pattern matching in switch expressions across all phases.

- **Operator precedence in the grammar** runs highest-to-lowest within the
  `expression` rule (ANTLR 4 convention: first alternative = highest precedence).
  Current order: `NOT > * / > + - > comparisons > OR > AND`.

- **Records as the in-memory type.** The interpreter and generated code both
  represent stream records as Jackson `ObjectNode`. This avoids generating
  typed POJOs and keeps the code simple without a type system for record shapes.

- **Code generation targets the DataStream API** (not Flink SQL) because the
  grammar has no output schema declaration, making SQL's `CREATE TABLE`
  impossible to generate without adding schema to the sink.

- **`AstVisitor<T>` dispatch helpers** (`visitExpr`, `visitTransform`) use
  sealed-interface switch so callers never write instanceof chains.

---

## Dependencies

| Dependency | Scope | Purpose |
|---|---|---|
| `antlr4-runtime 4.13.1` | compile | Run the generated lexer/parser |
| `jackson-databind 2.15.2` | compile | JSON record handling in interpreter |
| `junit-jupiter 5.10.2` | test | Unit tests |
| ANTLR4 Maven plugin | build | Generate parser from grammar |
| maven-assembly-plugin | build | Fat jar for standalone execution |
