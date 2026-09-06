package dev.bounded.compiler.api;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.nio.file.Path;
import java.util.List;

public record CompilationResult(
        List<Diagnostic> diagnostics,
        List<Path> generatedSourceFiles,
        List<Path> generatedClassFiles
) {
    public CompilationResult {
        diagnostics = List.copyOf(diagnostics);
        generatedSourceFiles = List.copyOf(generatedSourceFiles);
        generatedClassFiles = List.copyOf(generatedClassFiles);
    }

    public boolean succeeded() {
        return diagnostics.isEmpty();
    }
}
