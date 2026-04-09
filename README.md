# Flink DSL

A domain-specific language for describing Apache Flink stream processing pipelines declaratively. Write a high-level pipeline description and the compiler translates it into a runnable Flink 2.2 DataStream API job.

---

## Overview

Flink DSL lets you describe what your data pipeline should do — where data comes from, how it should be filtered and transformed, and where it should go — without writing boilerplate Flink code. The compiler handles the translation.

**Compiler pipeline:**
```
DSL program → Lexer/Parser → AST → Semantic Analysis → Dataflow Graph → Flink Job
```

The system also includes a **local interpreter** that runs pipelines directly on JSON files, so you can test your DSL programs without a Flink cluster.

---

## Requirements

- Java 25
- Maven 3.x

---

## Build

```bash
mvn package
```

This generates the ANTLR parser from the grammar and produces a fat jar at:
```
target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## Usage

### Generate a Flink job

Compiles a `.flink` program into a Flink DataStream API Java source file.

```bash
java -jar target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
     <program.flink> [output-dir]
```

- `output-dir` defaults to `./generated/` if not specified.
- Produces one `<PipelineName>Job.java` file per pipeline in the program.

**Example:**
```bash
java -jar target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar examples/hello.flink
# → generated/ClickstreamJob.java
```

### Run locally on files

Executes the pipeline on a local JSON-lines file without needing a Flink cluster. Useful for testing.

```bash
java -jar target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
     --run <program.flink> <input.jsonl> <output.jsonl>
```

Each line of the input file must be a JSON object whose fields match the source schema.

**Example:**
```bash
java -jar target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar \
     --run examples/hello.flink examples/input.jsonl examples/output.jsonl
```

---

## Language Reference

### Program structure

A program is one or more `pipeline` definitions.

```
pipeline <name> {
    parallelism <n>

    source <streamName> from <connector> schema {
        <field> : <type>
        ...
    }

    <transform>
    ...

    sink to <connector> format <format>
}
```

---

### Parallelism

Sets the number of parallel instances Flink will use for this pipeline.

```
parallelism 4
```

Must be an integer ≥ 1.

---

### Source

Declares where data comes from and what its fields look like.

```
source <streamName> from <connector> schema {
    <field> : <type>
    ...
}
```

- `streamName` — the identifier used to reference fields in expressions throughout the pipeline (e.g. `events`).
- At least one field must be declared in the schema.

**Connectors:**

| Connector | Syntax | Description |
|---|---|---|
| Kafka | `kafka("<topic>")` | Reads JSON messages from a Kafka topic |
| File  | `file("<path>")` | Reads JSON lines from a local file |

**Schema types:**

| Type | Description |
|---|---|
| `string` | UTF-8 text |
| `int` | 32-bit integer |
| `long` | 64-bit integer |
| `double` | 64-bit floating point |
| `float` | 32-bit floating point |
| `boolean` | `true` or `false` |

**Example:**
```
source events from kafka("user-clicks") schema {
    userId    : string
    amount    : double
    timestamp : long
}
```

---

### Transforms

Transforms are applied in the order they appear. The pipeline supports three kinds.

#### filter

Keeps only records for which the expression evaluates to `true`.

```
filter <expression>
```

The expression must be boolean-typed. Multiple `filter` statements may appear and are applied in sequence.

**Example:**
```
filter events.amount > 0
filter events.userId != ""
```

#### map

Replaces each record with a new record whose fields are defined by the assignment block.

```
map {
    <field> = <expression>
    ...
}
```

**Example:**
```
map {
    userId = events.userId
    amount = events.amount * 1.1
}
```

#### flatMap

Same as `map` but designed for operations that produce one output record per input record with a different shape. Uses Flink's `FlatMapFunction` under the hood.

```
flatMap {
    <field> = <expression>
    ...
}
```

---

### Sink

Declares where output records are written.

```
sink to <connector> format <format>
```

**Formats:**

| Format | Description |
|---|---|
| `json` | Writes each record as a JSON object |
| `csv` | Writes each record in CSV format |

**Example:**
```
sink to kafka("enriched-clicks") format json
sink to file("output/results.jsonl") format json
```

---

### Expressions

Expressions appear in `filter` predicates and on the right-hand side of `map`/`flatMap` assignments.

#### Field access

```
<streamName>.<fieldName>
```

`streamName` must match the identifier declared in the `source` statement. `fieldName` must be declared in the schema.

```
events.amount
events.userId
```

#### Arithmetic

```
events.amount + 100
events.price * events.quantity
events.total - events.discount
events.amount / 12.0
```

Both operands must be numeric (`int`, `long`, `float`, or `double`). Mixed numeric types are promoted to the wider type.

#### Comparisons

```
events.amount > 0
events.userId == "admin"
events.count != 0
events.score >= 9.5
```

Numeric types can be compared with `== != < > <= >=`. Strings support only `==` and `!=`, which use value equality.

#### Boolean logic

```
events.amount > 0 and events.userId != ""
events.flagA or events.flagB
not events.isDeleted
```

`and` and `or` require boolean operands. `not` requires a boolean operand.

#### Parentheses

```
(events.a + events.b) * events.c
```

#### Literals

| Kind | Examples |
|---|---|
| Integer | `0`, `42`, `1000` |
| Float | `1.5`, `3.14`, `0.001` |
| String | `"hello"`, `""`, `"user@example.com"` |
| Boolean | `true`, `false` |

#### Operator precedence (high to low)

| Precedence | Operators |
|---|---|
| Highest | `not` (unary) |
| | `*` `/` |
| | `+` `-` |
| | `==` `!=` `<` `>` `<=` `>=` |
| | `or` |
| Lowest | `and` |

---

### Comments

```
// This is a single-line comment

/*
   This is a
   block comment
*/
```

---

## Full Example

```
pipeline clickstream {
    parallelism 4

    source events from kafka("clicks") schema {
        userId : string
        amount : double
        ts     : long
    }

    // Drop records with empty user IDs or non-positive amounts
    filter events.userId != ""
    filter events.amount > 0

    // Apply a 10% markup
    map {
        userId = events.userId
        amount = events.amount * 1.1
    }

    sink to kafka("enriched-clicks") format json
}
```

---

## Error reporting

The compiler reports all errors before aborting, so you see every issue at once.

**Syntax errors** are reported with line and column numbers:
```
[Parse] 6:4 — mismatched input 'filer' expecting {'filter', 'map', 'flatMap', 'sink'}
```

**Semantic errors** are reported with the pipeline name for context:
```
[SemanticError] [clickstream] unknown field 'price' — not declared in schema
[SemanticError] [clickstream] '+'/'-' requires numeric operands, got STRING
```

---

## Project structure

```
src/
├── main/
│   ├── antlr4/com/flinkdsl/grammar/
│   │   └── FlinkPipeline.g4        ← grammar
│   └── java/com/flinkdsl/
│       ├── Main.java                ← CLI entry point
│       ├── ast/                     ← typed AST nodes
│       ├── analysis/                ← semantic analysis
│       ├── ir/                      ← dataflow graph IR
│       ├── codegen/                 ← Flink job code generation
│       └── interpreter/             ← local file-based execution
examples/
├── hello.flink                      ← example DSL program
├── input.jsonl                      ← sample input for --run mode
└── output.jsonl                     ← produced by --run mode
generated/
└── ClickstreamJob.java              ← output of code generation
```
