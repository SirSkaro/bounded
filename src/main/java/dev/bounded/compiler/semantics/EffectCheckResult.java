package dev.bounded.compiler.semantics;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.List;

import static java.util.List.copyOf;

public record EffectCheckResult(List<Diagnostic> diagnostics) {
    public EffectCheckResult {
        diagnostics = copyOf(diagnostics);
    }

    public static EffectCheckResult validResult() {
        return new EffectCheckResult(List.of());
    }

    public boolean isValid() {
        return diagnostics.isEmpty();
    }
}
