package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.DUPLICATE_OPERATION;

final class DeclarationChecker implements SemanticRule {
    @Override
    public List<Diagnostic> check(BoundedProgram program) {
        Set<String> names = new HashSet<>();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (var operation : program.archeClass().operations()) {
            if (!names.add(operation.name())) {
                diagnostics.add(new Diagnostic(DUPLICATE_OPERATION, operation.span(),
                        "Operation '" + operation.name() + "' is already declared in '"
                                + program.archeClass().name() + "'."));
            }
        }
        return List.copyOf(diagnostics);
    }
}
