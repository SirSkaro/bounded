package dev.bounded.compiler.acceptance;

import dev.bounded.compiler.api.SourceFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class AcceptanceSources {
    private AcceptanceSources() {}

    public static SourceFile fixture(String resource) throws IOException {
        return new SourceFile(Path.of(resource).getFileName().toString(), read(resource));
    }

    public static List<String> expectedDiagnostics(String resource) throws IOException {
        return read(resource).lines().toList();
    }

    private static String read(String resource) throws IOException {
        try (var input = AcceptanceSources.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing test resource: " + resource);
            }
            return new String(input.readAllBytes(), UTF_8);
        }
    }
}
