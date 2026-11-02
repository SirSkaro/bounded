package dev.bounded.compiler.api.write;

import java.nio.file.Path;
import java.util.List;

import static java.util.List.copyOf;

public record WriteResult(List<Path> generatedClassFiles) {
    public WriteResult {
        generatedClassFiles = copyOf(generatedClassFiles);
    }
}
