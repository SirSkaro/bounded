package dev.bounded.compiler.ast;

import java.util.Objects;

public record ArcheOperationDeclaration(
        ArcheOperationKind kind,
        String name,
        SourceSpan span
) {
    public ArcheOperationDeclaration {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(span, "span");
    }
}
