package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.List;
import java.util.Optional;

import static java.util.List.copyOf;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public record ParseResult(Optional<BoundedProgram> program, List<Diagnostic> diagnostics) {
    public ParseResult {
        diagnostics = copyOf(diagnostics);
    }

    public static ParseResult validResult(BoundedProgram program) {
        return new ParseResult(of(program), List.of());
    }

    public static ParseResult invalidResult(List<Diagnostic> diagnostics) {
        return new ParseResult(empty(), diagnostics);
    }

}
