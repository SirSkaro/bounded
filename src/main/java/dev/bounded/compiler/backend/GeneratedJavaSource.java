package dev.bounded.compiler.backend;


import static java.util.Objects.requireNonNull;

public record GeneratedJavaSource(String qualifiedClassName, String content) {
    public GeneratedJavaSource {
        requireNonNull(qualifiedClassName, "qualifiedClassName");
        requireNonNull(content, "content");
    }
}
