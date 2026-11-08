package dev.bounded.compiler.api.generate;

import static java.util.Objects.requireNonNull;

public record GeneratedClass(ClassName className, byte[] bytecode) {
    public GeneratedClass {
        requireNonNull(className, "className");
        bytecode = bytecode.clone();
    }

    public GeneratedClass(String qualifiedClassName, byte[] bytecode) {
        this(new ClassName(qualifiedClassName), bytecode);
    }

    public String qualifiedClassName() {
        return className.binaryName();
    }

    @Override
    public byte[] bytecode() {
        return bytecode.clone();
    }
}
