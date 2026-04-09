package com.flinkdsl.ast;

/** sink to <connector> format <formatType> */
public record SinkNode(ConnectorNode connector, FormatType format) {}
