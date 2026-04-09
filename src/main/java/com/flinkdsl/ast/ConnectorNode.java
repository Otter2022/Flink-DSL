package com.flinkdsl.ast;

public sealed interface ConnectorNode
        permits ConnectorNode.KafkaConnector, ConnectorNode.FileConnector {

    record KafkaConnector(String topic) implements ConnectorNode {}
    record FileConnector(String path)   implements ConnectorNode {}
}
