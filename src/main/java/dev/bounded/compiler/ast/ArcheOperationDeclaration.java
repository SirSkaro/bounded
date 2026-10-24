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
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        requireNonNull(span, "span");
    }
}
