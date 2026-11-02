package dev.bounded.compiler.api.check;

import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.List;

import static java.util.List.copyOf;

public record CheckResult(List<Diagnostic> diagnostics) {
    public CheckResult {
        diagnostics = copyOf(diagnostics);
    }

    public static CheckResult validResult() {
        return new CheckResult(List.of());
    }

    public boolean isValid() {
        return diagnostics.isEmpty();
    }
}
