package dev.bounded.compiler.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixtureCompilationTest {
    private final BoundedCompiler compiler = new BoundedCompiler();

    @TempDir
    Path temporaryDirectory;

    @Test
    void compilesAValidBoundedFixtureIntoALoadableClass() throws Exception {
        var result = compiler.compile(
                fixtureSource("cases/valid/money.bounded"),
                temporaryDirectory.resolve("valid"));

        assertTrue(result.succeeded());
        assertEquals(1, result.generatedSourceFiles().size());
        assertEquals(1, result.generatedClassFiles().size());
        assertTrue(Files.exists(result.generatedSourceFiles().getFirst()));
        assertTrue(Files.exists(result.generatedClassFiles().getFirst()));

        var classesRoot = temporaryDirectory.resolve("valid/classes");
        try (var classLoader = new URLClassLoader(
                new java.net.URL[]{classesRoot.toUri().toURL()})) {
            var generatedClass = classLoader.loadClass("bounded.generated.Bounded_Money");
            assertEquals(
                    "VALUE",
                    generatedClass.getMethod("boundedArcheClassKind").invoke(null));

            @SuppressWarnings("unchecked")
            var operations = (Map<String, String>) generatedClass
                    .getMethod("boundedOperations")
                    .invoke(null);
            assertEquals(Map.of("format", "FUNCTION"), operations);
        }
    }

    @Test
    void matchesTheExpectedDiagnosticAndEmitsNoArtifactsForAnInvalidFixture()
            throws Exception {
        var outputRoot = temporaryDirectory.resolve("invalid");
        var result = compiler.compile(
                fixtureSource("cases/invalid/value-query.bounded"),
                outputRoot);

        assertFalse(result.succeeded());
        var actualDiagnostics = result.diagnostics().stream()
                .map(diagnostic -> diagnostic.format())
                .toList();
        var expectedDiagnostics = Files.readAllLines(
                fixturePath("cases/invalid/value-query.diag"),
                StandardCharsets.UTF_8);

        assertEquals(expectedDiagnostics, actualDiagnostics);
        assertTrue(result.generatedSourceFiles().isEmpty());
        assertTrue(result.generatedClassFiles().isEmpty());
        assertFalse(Files.exists(outputRoot));
    }

    private SourceFile fixtureSource(String resourceName) throws Exception {
        var path = fixturePath(resourceName);
        return new SourceFile(
                path.getFileName().toString(),
                Files.readString(path, StandardCharsets.UTF_8));
    }

    private Path fixturePath(String resourceName) throws Exception {
        var resource = getClass().getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IllegalArgumentException("Missing test resource: " + resourceName);
        }
        return Path.of(resource.toURI());
    }
}
