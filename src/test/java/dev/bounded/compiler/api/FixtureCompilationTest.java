package dev.bounded.compiler.api;

import dev.bounded.compiler.locale.LocaleSensitiveTest;
import org.junit.jupiter.api.parallel.Isolated;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Isolated
class FixtureCompilationTest {
    private final BoundedCompiler compiler = new BoundedCompiler();

    @TempDir
    Path temporaryDirectory;

    @LocaleSensitiveTest
    @DisplayName("Compiles a valid fixture independently of the default locale")
    void compilesAValidBoundedFixtureIntoALoadableClass() throws Exception {
        var result = compiler.compile(
                fixtureSource("cases/valid/money.bounded"),
                temporaryDirectory.resolve("valid"));

        assertThat(result.succeeded(), is(true));
        assertThat(result.generatedSourceFiles(), hasSize(1));
        assertThat(result.generatedClassFiles(), hasSize(1));
        assertThat(Files.exists(result.generatedSourceFiles().getFirst()), is(true));
        assertThat(Files.exists(result.generatedClassFiles().getFirst()), is(true));

        var classesRoot = temporaryDirectory.resolve("valid/classes");
        try (var classLoader = new URLClassLoader(
                new java.net.URL[]{classesRoot.toUri().toURL()})) {
            var generatedClass = classLoader.loadClass("bounded.generated.Bounded_Money");
            assertThat(generatedClass.getMethod("boundedArcheClassKind").invoke(null), is("VALUE"));

            @SuppressWarnings("unchecked")
            var operations = (Map<String, String>) generatedClass
                    .getMethod("boundedOperations")
                    .invoke(null);
            assertThat(operations, is(Map.of("format", "FUNCTION")));
        }
    }

    @LocaleSensitiveTest
    @DisplayName("Produces stable diagnostics and no artifacts for an invalid fixture")
    void matchesTheExpectedDiagnosticAndEmitsNoArtifactsForAnInvalidFixture()
            throws Exception {
        var outputRoot = temporaryDirectory.resolve("invalid");
        var result = compiler.compile(
                fixtureSource("cases/invalid/value-query.bounded"),
                outputRoot);

        assertThat(result.succeeded(), is(false));
        var actualDiagnostics = result.diagnostics().stream()
                .map(diagnostic -> diagnostic.format())
                .toList();
        var expectedDiagnostics = Files.readAllLines(
                fixturePath("cases/invalid/value-query.diag"),
                StandardCharsets.UTF_8);

        assertThat(actualDiagnostics, is(expectedDiagnostics));
        assertThat(result.generatedSourceFiles(), is(empty()));
        assertThat(result.generatedClassFiles(), is(empty()));
        assertThat(Files.exists(outputRoot), is(false));
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
