package dev.bounded.compiler.backend;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.nio.file.Path;
import java.util.List;

import static java.util.List.copyOf;

public record JavaClassCompilation(
        List<Path> generatedSourceFiles,
        List<Path> generatedClassFiles,
        List<Diagnostic> diagnostics
) {
    public JavaClassCompilation {
        generatedSourceFiles = copyOf(generatedSourceFiles);
        generatedClassFiles = copyOf(generatedClassFiles);
        diagnostics = copyOf(diagnostics);
    }
}
