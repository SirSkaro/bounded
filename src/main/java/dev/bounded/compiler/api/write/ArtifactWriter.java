package dev.bounded.compiler.api.write;

import dev.bounded.compiler.api.generate.GeneratedProgram;

import java.io.IOException;

@FunctionalInterface
public interface ArtifactWriter {
    WriteResult write(GeneratedProgram generatedProgram, OutputTarget outputTarget) throws IOException;
}
