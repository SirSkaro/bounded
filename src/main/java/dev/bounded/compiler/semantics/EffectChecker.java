package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.diagnostics.DiagnosticCode;

import java.util.List;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

public final class EffectChecker implements SemanticRule {

    @Override
    public List<Diagnostic> check(BoundedProgram program) {
        var archeClass = program.archeClass();
        return archeClass.operations().stream()
                .filter(not(archeOp -> effectAllowed(archeClass, archeOp)))
                .map(archeOp -> constructDiagnostic(archeClass, archeOp))
                .toList();

    }

    private boolean effectAllowed(ArcheClassDeclaration archeClass, ArcheOperationDeclaration operation) {
        return archeClass.kind().allows(operation.kind());
    }

    private Diagnostic constructDiagnostic(ArcheClassDeclaration archeClass, ArcheOperationDeclaration operation) {
        var message = format("%s '%s' permits operations through %s; '%s' is %s.",
            archeClass.kind().label(),
            archeClass.name(),
            archeClass.kind().maximumOperationKind().metadataName(),
            operation.name(),
            operation.kind().metadataName()
        );

        return new Diagnostic(
            DiagnosticCode.EFFECT_NOT_ALLOWED,
            operation.span(),
            message
        );
    }
}
