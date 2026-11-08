package dev.bounded.compiler.ast;

import static java.util.Objects.requireNonNull;

public record SourceSpan(String sourceName, int line, int column) {
    public SourceSpan {
        requireNonNull(sourceName, "sourceName");
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("Source positions are one-based");
        }
    }

    public SourceSpan(int line, int column) {
        this("<unknown>", line, column);
    }
}
