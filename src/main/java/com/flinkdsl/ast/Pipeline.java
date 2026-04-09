package com.flinkdsl.ast;

import java.util.List;

/**
 * pipeline <name> {
 *     parallelism <n>
 *     source …
 *     (transform)+
 *     sink …
 * }
 */
public record Pipeline(
        String name,
        int parallelism,
        SourceNode source,
        List<TransformNode> transforms,
        SinkNode sink
) {}
