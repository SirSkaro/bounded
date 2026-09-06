package dev.bounded.compiler.backend;

import java.util.Objects;

public record GeneratedJavaSource(String qualifiedClassName, String content) {
    public GeneratedJavaSource {
        Objects.requireNonNull(qualifiedClassName, "qualifiedClassName");
        Objects.requireNonNull(content, "content");
    }
}
