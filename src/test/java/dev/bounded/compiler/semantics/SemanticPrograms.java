package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.SourceSpan;

import java.util.List;

final class SemanticPrograms {
    private SemanticPrograms() {}

    static BoundedProgram value(ArcheOperationDeclaration... operations) {
        return new BoundedProgram(new ArcheClassDeclaration(ArcheClassKind.VALUE, "Money",
                List.of(operations), new SourceSpan("money.bounded", 1, 1)));
    }

    static ArcheOperationDeclaration operation(ArcheOperationKind kind, String name, int line) {
        return new ArcheOperationDeclaration(kind, name, new SourceSpan("money.bounded", line, 3));
    }
}
