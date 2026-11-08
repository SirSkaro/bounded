package dev.bounded.compiler.api.check;

import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.ast.AnalyzedProgram;

import java.util.List;

import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

public final class CheckResult {
    private final AnalyzedProgram program;
    private final List<Diagnostic> diagnostics;

    private CheckResult(AnalyzedProgram program, List<Diagnostic> diagnostics) {
        this.program = program;
        this.diagnostics = copyOf(diagnostics);
    }

    public static CheckResult validResult(AnalyzedProgram program) {
        return new CheckResult(requireNonNull(program, "program"), List.of());
    }

    public static CheckResult invalidResult(List<Diagnostic> diagnostics) {
        if (diagnostics.isEmpty()) {
            throw new IllegalArgumentException("An invalid check result requires diagnostics");
        }
        return new CheckResult(null, diagnostics);
    }

    public boolean isValid() {
        return program != null;
    }

    public AnalyzedProgram program() {
        return program;
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
