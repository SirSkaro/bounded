package dev.bounded.compiler.ast;

import java.util.List;
import java.util.Objects;

public record ArcheClassDeclaration(
        ArcheClassKind kind,
        String name,
        List<ArcheOperationDeclaration> operations,
        SourceSpan span
) {
    public ArcheClassDeclaration {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(name, "name");
        operations = List.copyOf(operations);
        Objects.requireNonNull(span, "span");
    }
}
