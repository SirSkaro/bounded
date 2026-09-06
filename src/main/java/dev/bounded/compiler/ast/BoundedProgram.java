package dev.bounded.compiler.ast;

import java.util.Objects;

public record BoundedProgram(ArcheClassDeclaration archeClass) {
    public BoundedProgram {
        Objects.requireNonNull(archeClass, "archeClass");
    }
}
