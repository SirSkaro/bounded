package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.List;

import static java.util.List.copyOf;
import static java.util.Objects.requireNonNull;

public final class ParseResult {
    private final BoundedProgram program;
    private final List<Diagnostic> diagnostics;

    private ParseResult(BoundedProgram program, List<Diagnostic> diagnostics) {
        this.program = program;
        this.diagnostics = copyOf(diagnostics);
    }

    public static ParseResult validResult(BoundedProgram program) {
        requireNonNull(program, "A valid parse result requires the successfully parsed program.");
        return new ParseResult(program, List.of());
    }

    public static ParseResult invalidResult(List<Diagnostic> diagnostics) {
        requireNonNull(diagnostics);
        if (diagnostics.isEmpty()) {
            throw new IllegalArgumentException("An invalid parse result requires diagnostics");
        }
        return new ParseResult(null, diagnostics);
    }

    public boolean isValid() {
        return program != null;
    }

    public BoundedProgram program() {
        return program;
    }

    public List<Diagnostic> diagnostics() {
        return diagnostics;
    }
}
