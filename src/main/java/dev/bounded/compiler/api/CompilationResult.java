package dev.bounded.compiler.api;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.nio.file.Path;
import java.util.List;

import static java.util.List.copyOf;

public record CompilationResult(
        List<Diagnostic> diagnostics,
        List<Path> generatedClassFiles
) {
    public CompilationResult {
        diagnostics = copyOf(diagnostics);
        generatedClassFiles = copyOf(generatedClassFiles);
    }

    public boolean succeeded() {
        return diagnostics.isEmpty();
    }
}
