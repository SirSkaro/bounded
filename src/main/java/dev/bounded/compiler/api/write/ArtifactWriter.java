package dev.bounded.compiler.api.write;

import dev.bounded.compiler.api.generate.GeneratedClass;

import java.io.IOException;

@FunctionalInterface
public interface ArtifactWriter {
    WriteResult write(GeneratedClass generatedClass, OutputTarget outputTarget) throws IOException;
}
