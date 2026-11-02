package dev.bounded.compiler.cli;

import dev.bounded.compiler.api.CompilationRequest;
import dev.bounded.compiler.api.CompilationResult;
import dev.bounded.compiler.api.CompileProgram;
import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.api.write.OutputTarget;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.locale.LocaleSensitiveTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.JVM_BACKEND_FAILURE;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.SYNTAX_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Isolated
@DisplayName("Compiler fixtures through the production composition root")
class FixtureCompilationTest {
    private final CompileProgram compiler = Main.compiler();

    @TempDir
    Path temporaryDirectory;

    @LocaleSensitiveTest
    @DisplayName("Compiles a valid fixture independently of the default locale")
    void compilesAValidBoundedFixtureIntoALoadableClass() throws Exception {
        var result = compile(
                fixtureSource("cases/valid/money.bounded"),
                temporaryDirectory.resolve("valid"));

        assertThat(result.succeeded(), is(true));
        assertThat(result.generatedClassFiles(), hasSize(1));
        assertThat(Files.exists(temporaryDirectory.resolve("valid/generated-sources")), is(false));
        assertThat(Files.exists(result.generatedClassFiles().getFirst()), is(true));

        var classesRoot = temporaryDirectory.resolve("valid/classes");
        try (var classLoader = new URLClassLoader(
                new java.net.URL[]{classesRoot.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            var generatedClass = classLoader.loadClass("bounded.generated.Bounded_Money");
            assertThat(generatedClass.getMethod("boundedArcheClassKind").invoke(null), is("VALUE"));

            @SuppressWarnings("unchecked")
            var operations = (Map<String, String>) generatedClass
                    .getMethod("boundedOperations")
                    .invoke(null);
            assertThat(operations, is(Map.of("format", "FUNCTION")));
        }
    }

    @Test
    @DisplayName("Produces stable diagnostics and no artifacts for an invalid fixture")
    void matchesTheExpectedDiagnosticAndEmitsNoArtifactsForAnInvalidFixture()
            throws Exception {
        var outputRoot = temporaryDirectory.resolve("invalid");
        var result = compile(
                fixtureSource("cases/invalid/value-query.bounded"),
                outputRoot);

        assertThat(result.succeeded(), is(false));
        var actualDiagnostics = result.diagnostics().stream()
                .map(Diagnostic::format)
                .toList();
        var expectedDiagnostics = Files.readAllLines(
                fixturePath("cases/invalid/value-query.diag"),
                StandardCharsets.UTF_8);

        assertThat(actualDiagnostics, is(expectedDiagnostics));
        assertThat(result.generatedClassFiles(), is(empty()));
        assertThat(Files.exists(outputRoot), is(false));
    }

    @Test
    @DisplayName("Syntax errors produce diagnostics and no output directory")
    void stopsAfterSyntaxErrors() throws IOException {
        var outputRoot = temporaryDirectory.resolve("syntax-error");

        var result = compile(new SourceFile("Amount.bounded", "value Amount {"), outputRoot);

        assertThat(result.succeeded(), is(false));
        assertThat(result.diagnostics().getFirst().code(), is(SYNTAX_ERROR));
        assertThat(result.generatedClassFiles(), is(empty()));
        assertThat(Files.exists(outputRoot), is(false));
    }

    @Test
    @DisplayName("Bytecode generation failures cross the ports as BND9001 without artifacts")
    void reportsBytecodeGenerationFailure() throws IOException {
        var outputRoot = temporaryDirectory.resolve("oversized");
        var source = new SourceFile("oversized.bounded", "\n  value " + "A".repeat(65_536) + " {}");

        var result = compile(source, outputRoot);

        assertThat(result.succeeded(), is(false));
        var diagnostic = result.diagnostics().getFirst();
        assertThat(diagnostic.code(), is(JVM_BACKEND_FAILURE));
        assertThat(diagnostic.code().externalCode(), is("BND9001"));
        assertThat(diagnostic.span(), is(new SourceSpan(2, 3)));
        assertThat(diagnostic.message(), startsWith("Could not generate JVM bytecode: "));
        assertThat(result.generatedClassFiles(), is(empty()));
        assertThat(Files.exists(outputRoot), is(false));
    }

    @Test
    @DisplayName("Filesystem failures propagate through the configured compiler")
    void propagatesFilesystemFailures() throws IOException {
        var outputRoot = Files.writeString(temporaryDirectory.resolve("occupied"), "occupied");
        var source = new SourceFile("Amount.bounded", "value Amount {}");

        assertThrows(IOException.class, () -> compile(source, outputRoot));
    }

    private SourceFile fixtureSource(String resourceName) throws Exception {
        var path = fixturePath(resourceName);
        return new SourceFile(
                path.getFileName().toString(),
                Files.readString(path, StandardCharsets.UTF_8));
    }

    private CompilationResult compile(SourceFile sourceFile, Path outputRoot) throws IOException {
        return compiler.compile(new CompilationRequest(sourceFile, new OutputTarget(outputRoot)));
    }

    private Path fixturePath(String resourceName) throws Exception {
        var resource = getClass().getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IllegalArgumentException("Missing test resource: " + resourceName);
        }
        return Path.of(resource.toURI());
    }
}
