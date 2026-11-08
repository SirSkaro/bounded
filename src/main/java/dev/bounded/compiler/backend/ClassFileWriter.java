package dev.bounded.compiler.backend;

import dev.bounded.compiler.api.generate.GeneratedProgram;
import dev.bounded.compiler.api.write.ArtifactWriter;
import dev.bounded.compiler.api.write.CompiledClass;
import dev.bounded.compiler.api.write.OutputTarget;
import dev.bounded.compiler.api.write.WriteResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ClassFileWriter implements ArtifactWriter {
    @Override
    public WriteResult write(GeneratedProgram program, OutputTarget outputTarget) throws IOException {
        Files.createDirectories(outputTarget.root());
        var classesRoot = directory(outputTarget.root().toRealPath(), "classes");
        List<CompiledClass> artifacts = new ArrayList<>();
        for (var generatedClass : program.classes()) {
            var artifact = new CompiledClass(generatedClass.className(), classesRoot);
            prepareParent(classesRoot, artifact.classFile());
            rejectSymbolicLink(artifact.classFile());
            Files.write(artifact.classFile(), generatedClass.bytecode());
            artifacts.add(artifact);
        }
        return new WriteResult(artifacts);
    }

    private void prepareParent(Path root, Path classFile) throws IOException {
        var relative = root.relativize(classFile);
        var parent = root;
        for (int index = 0; index < relative.getNameCount() - 1; index++) {
            parent = directory(parent, relative.getName(index).toString());
        }
    }

    private Path directory(Path parent, String name) throws IOException {
        var directory = parent.resolve(name);
        rejectSymbolicLink(directory);
        return Files.createDirectories(directory);
    }

    private void rejectSymbolicLink(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            throw new IOException("Cannot write through symbolic link: " + path);
        }
    }
}
