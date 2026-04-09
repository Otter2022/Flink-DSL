package com.flinkdsl.ir;

/** A directed edge from one node to another, identified by node ID. */
public record DataflowEdge(String fromId, String toId) {}
