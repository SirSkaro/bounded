package dev.bounded.compiler.ast;

import static java.util.Objects.requireNonNull;

public record ArcheOperationDeclaration(
        ArcheOperationKind kind,
        String name,
        SourceSpan span
) {
    public ArcheOperationDeclaration {
        requireNonNull(kind, "kind");
        requireNonNull(name, "name");
        requireNonNull(span, "span");
    }
}
