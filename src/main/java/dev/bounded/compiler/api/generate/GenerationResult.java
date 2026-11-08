package dev.bounded.compiler.api.generate;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.List;

import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

public final class GenerationResult {
    private final GeneratedProgram generatedProgram;
    private final List<Diagnostic> diagnostics;

    private GenerationResult(GeneratedProgram generatedProgram, List<Diagnostic> diagnostics) {
        this.generatedProgram = generatedProgram;
        this.diagnostics = copyOf(diagnostics);
    }

    public static GenerationResult validResult(GeneratedProgram generatedProgram) {
        requireNonNull(generatedProgram, "A valid generation result requires the generated program.");
        return new GenerationResult(generatedProgram, List.of());
    }

    public static GenerationResult invalidResult(List<Diagnostic> diagnostics) {
        requireNonNull(diagnostics);
        if (diagnostics.isEmpty()) {
            throw new IllegalArgumentException("An invalid generation result requires diagnostics");
        }
        return new GenerationResult(null, diagnostics);
    }

    public boolean isValid() {
        return generatedProgram != null;
    }

    public GeneratedProgram generatedProgram() {
        return generatedProgram;
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
