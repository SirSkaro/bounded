package dev.bounded.compiler.api.generate;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.List;

import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

public final class GenerationResult {
    private final GeneratedClass generatedClass;
    private final List<Diagnostic> diagnostics;

    private GenerationResult(GeneratedClass generatedClass, List<Diagnostic> diagnostics) {
        this.generatedClass = generatedClass;
        this.diagnostics = copyOf(diagnostics);
    }

    public static GenerationResult validResult(GeneratedClass generatedClass) {
        requireNonNull(generatedClass, "A valid generation result requires the generated class.");
        return new GenerationResult(generatedClass, List.of());
    }

    public static GenerationResult invalidResult(List<Diagnostic> diagnostics) {
        requireNonNull(diagnostics);
        if (diagnostics.isEmpty()) {
            throw new IllegalArgumentException("An invalid generation result requires diagnostics");
        }
        return new GenerationResult(null, diagnostics);
    }

    public boolean isValid() {
        return generatedClass != null;
    }

    public GeneratedClass generatedClass() {
        return generatedClass;
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
