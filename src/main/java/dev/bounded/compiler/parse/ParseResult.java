package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.List;
import java.util.Optional;

public record ParseResult(
        Optional<BoundedProgram> program,
        List<Diagnostic> diagnostics
) {
    public ParseResult {
        program = program == null ? Optional.empty() : program;
        diagnostics = List.copyOf(diagnostics);
    }
}
