package dev.bounded.compiler.api;

import dev.bounded.compiler.api.write.OutputTarget;

import static java.util.Objects.requireNonNull;

public record CompilationRequest(SourceFile sourceFile, OutputTarget outputTarget) {
    public CompilationRequest {
        requireNonNull(sourceFile, "sourceFile");
        requireNonNull(outputTarget, "outputTarget");
    }
}
