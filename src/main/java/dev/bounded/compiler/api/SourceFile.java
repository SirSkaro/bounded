package dev.bounded.compiler.api;

import static java.util.Objects.requireNonNull;

public record SourceFile(String name, String content) {
    public SourceFile {
        requireNonNull(name, "name");
        requireNonNull(content, "content");
    }
}
