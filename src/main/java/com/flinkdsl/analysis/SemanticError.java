package com.flinkdsl.analysis;

/**
 * A single semantic error produced during analysis.
 * Always prefixed with the pipeline name for context.
 */
public record SemanticError(String message) {
    @Override
    public String toString() {
        return "[SemanticError] " + message;
    }
}
