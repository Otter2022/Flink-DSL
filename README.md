# Flink DSL

## Current State of Project
TODO

## Usage of Visitor
TODO

## Assumed already installed

- Java 25 (JDK)
- Maven 3.x

## Installing required dependencies

No manual dependency installation is needed. Running `mvn package` will automatically download all required libraries (ANTLR4 runtime, Jackson, JUnit) from Maven Central on the first build.

## Build

```bash
mvn package
```

Produces a fat jar at:

```
target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Run

### Generate a Flink job source file

```bash
java -jar target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar <program.flink> [output-dir]
```

`output-dir` defaults to `./generated/` if omitted. Produces one `<PipelineName>Job.java` per pipeline.

**Example:**
```bash
java -jar target/flink-dsl-0.1.0-SNAPSHOT-jar-with-dependencies.jar examples/hello.flink
# → generated/ClickstreamJob.java
```

### Run locally on files (no Flink cluster needed)

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
