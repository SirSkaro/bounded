package dev.bounded.compiler.api;

import java.io.IOException;

@FunctionalInterface
public interface CompileProgram {
    CompilationResult compile(CompilationRequest request) throws IOException;
}
