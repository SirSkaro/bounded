package dev.bounded.compiler.api.write;

import java.util.List;

import static java.util.List.copyOf;

public record WriteResult(List<CompiledClass> artifacts) {
    public WriteResult {
        artifacts = copyOf(artifacts);
        if (artifacts.isEmpty()) {
            throw new IllegalArgumentException("A write result requires at least one artifact");
        }
    }
}
