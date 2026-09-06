package dev.bounded.compiler.ast;

public record SourceSpan(int line, int column) {
    public SourceSpan {
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("Source positions are one-based");
        }
    }
}
