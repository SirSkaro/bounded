package dev.bounded.compiler.api.write;

import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public record OutputTarget(Path root) {
    public OutputTarget {
        requireNonNull(root, "root");
    }
}
