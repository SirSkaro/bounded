package dev.bounded.compiler.ast;

import static java.util.Objects.requireNonNull;

public record BoundedProgram(ArcheClassDeclaration archeClass) {
    public BoundedProgram {
        requireNonNull(archeClass, "archeClass");
    }
}
