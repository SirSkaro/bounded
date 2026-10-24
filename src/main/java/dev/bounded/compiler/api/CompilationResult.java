package dev.bounded.compiler.api;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.nio.file.Path;
import java.util.List;

import static java.util.List.copyOf;

public record CompilationResult(
        List<Diagnostic> diagnostics,
        List<Path> generatedSourceFiles,
        List<Path> generatedClassFiles
) {
    public CompilationResult {
        diagnostics = copyOf(diagnostics);
        generatedSourceFiles = copyOf(generatedSourceFiles);
        generatedClassFiles = copyOf(generatedClassFiles);
    }

    public boolean succeeded() {
        return diagnostics.isEmpty();
    }
}
