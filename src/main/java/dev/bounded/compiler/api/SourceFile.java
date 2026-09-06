package dev.bounded.compiler.api;

import java.util.Objects;

public record SourceFile(String name, String content) {
    public SourceFile {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(content, "content");
    }
}
