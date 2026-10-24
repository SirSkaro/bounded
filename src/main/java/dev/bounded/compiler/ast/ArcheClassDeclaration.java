package dev.bounded.compiler.ast;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record ArcheClassDeclaration(
        ArcheClassKind kind,
        String name,
        List<ArcheOperationDeclaration> operations,
        SourceSpan span
) {
    public ArcheClassDeclaration {
        requireNonNull(kind, "kind");
        requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        operations = List.copyOf(operations);
        requireNonNull(span, "span");
    }
}
