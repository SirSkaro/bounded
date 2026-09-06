package dev.bounded.compiler.backend;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.nio.file.Path;
import java.util.List;

public record JavaClassCompilation(
        List<Path> generatedSourceFiles,
        List<Path> generatedClassFiles,
        List<Diagnostic> diagnostics
) {
    public JavaClassCompilation {
        generatedSourceFiles = List.copyOf(generatedSourceFiles);
        generatedClassFiles = List.copyOf(generatedClassFiles);
        diagnostics = List.copyOf(diagnostics);
    }
}
