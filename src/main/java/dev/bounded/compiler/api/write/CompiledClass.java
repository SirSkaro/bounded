package dev.bounded.compiler.api.write;

import dev.bounded.compiler.api.generate.ClassName;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public record CompiledClass(ClassName className, Path classPathRoot) {
    public CompiledClass {
        requireNonNull(className, "className");
        requireNonNull(classPathRoot, "classPathRoot");
    }

    public Path classFile() {
        return classPathRoot.resolve(className.resourceName());
    }
}
