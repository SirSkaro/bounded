package dev.bounded.compiler.backend;

import static java.util.Objects.requireNonNull;

public record GeneratedClass(String qualifiedClassName, byte[] bytecode) {
    public GeneratedClass {
        requireNonNull(qualifiedClassName, "qualifiedClassName");
        bytecode = bytecode.clone();
    }

    @Override
    public byte[] bytecode() {
        return bytecode.clone();
    }
}
