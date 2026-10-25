package dev.bounded.compiler.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClassFileWriter {
    public Path write(GeneratedClass generatedClass, Path outputRoot) throws IOException {
        var classPath = outputRoot.resolve("classes")
                .resolve(generatedClass.qualifiedClassName().replace('.', '/') + ".class");
        Files.createDirectories(classPath.getParent());
        Files.write(classPath, generatedClass.bytecode());
        return classPath;
    }
}
