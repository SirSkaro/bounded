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
        operations = List.copyOf(operations);
        requireNonNull(span, "span");
    }
}
