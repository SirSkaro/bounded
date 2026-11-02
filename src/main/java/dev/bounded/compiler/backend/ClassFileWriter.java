package dev.bounded.compiler.backend;

import dev.bounded.compiler.api.generate.GeneratedClass;
import dev.bounded.compiler.api.write.ArtifactWriter;
import dev.bounded.compiler.api.write.OutputTarget;
import dev.bounded.compiler.api.write.WriteResult;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public final class ClassFileWriter implements ArtifactWriter {
    @Override
    public WriteResult write(GeneratedClass generatedClass, OutputTarget outputTarget) throws IOException {
        var classPath = outputTarget.root().resolve("classes")
                .resolve(generatedClass.qualifiedClassName().replace('.', '/') + ".class");
        Files.createDirectories(classPath.getParent());
        Files.write(classPath, generatedClass.bytecode());
        return new WriteResult(List.of(classPath));
    }
}
