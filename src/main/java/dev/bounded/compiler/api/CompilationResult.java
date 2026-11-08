package dev.bounded.compiler.api;

import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.api.write.CompiledClass;

import java.util.List;

import static java.util.List.copyOf;

public final class CompilationResult {
    private final List<Diagnostic> diagnostics;
    private final List<CompiledClass> artifacts;

    private CompilationResult(List<Diagnostic> diagnostics, List<CompiledClass> artifacts) {
        this.diagnostics = copyOf(diagnostics);
        this.artifacts = copyOf(artifacts);
    }

    public static CompilationResult success(List<CompiledClass> artifacts) {
        if (artifacts.isEmpty()) {
            throw new IllegalArgumentException("Successful compilation requires artifacts");
        }
        return new CompilationResult(List.of(), artifacts);
    }

    public static CompilationResult failure(List<Diagnostic> diagnostics) {
        if (diagnostics.isEmpty()) {
            throw new IllegalArgumentException("Failed compilation requires diagnostics");
        }
        return new CompilationResult(diagnostics, List.of());
    }

    public boolean succeeded() {
        return diagnostics.isEmpty();
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    public List<CompiledClass> artifacts() {
        return artifacts;
    }
}
