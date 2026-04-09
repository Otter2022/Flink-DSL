package com.flinkdsl.ast;

import java.util.List;

/** Top-level node: one or more pipeline definitions. */
public record Program(List<Pipeline> pipelines) {}
