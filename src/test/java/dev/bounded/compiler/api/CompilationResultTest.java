package dev.bounded.compiler.api;

import dev.bounded.compiler.api.generate.ClassName;
import dev.bounded.compiler.api.write.CompiledClass;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.SYNTAX_ERROR;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Compilation result invariants")
class CompilationResultTest {
    private final Diagnostic diagnostic = new Diagnostic(SYNTAX_ERROR, new SourceSpan(1, 1), "Invalid syntax");
    private final CompiledClass artifact = new CompiledClass(new ClassName("Amount"), Path.of("classes"));

    private static Stream<Arguments> invalidLists() {
        return Stream.of(
                Arguments.of("a null list", null, NullPointerException.class),
                Arguments.of("a null entry", singletonList(null), NullPointerException.class),
                Arguments.of("an empty list", List.of(), IllegalArgumentException.class));
    }

    @DisplayName("Successful compilation requires artifacts")
    @ParameterizedTest(name = "Rejects {0} of artifacts")
    @MethodSource("invalidLists")
    void rejectsInvalidArtifacts(String description, List<CompiledClass> artifacts,
                                Class<? extends RuntimeException> exceptionType) {
        assertThrows(exceptionType, () -> CompilationResult.success(artifacts));
    }

    @DisplayName("Failed compilation requires diagnostics")
    @ParameterizedTest(name = "Rejects {0} of diagnostics")
    @MethodSource("invalidLists")
    void rejectsInvalidDiagnostics(String description, List<Diagnostic> diagnostics,
                                  Class<? extends RuntimeException> exceptionType) {
        assertThrows(exceptionType, () -> CompilationResult.failure(diagnostics));
    }

    @Test
    @DisplayName("Changes to the original diagnostics do not change the result")
    void copiesDiagnostics() {
        var diagnostics = new ArrayList<>(List.of(diagnostic));
        var result = CompilationResult.failure(diagnostics);
        diagnostics.clear();
        assertThat(result.diagnostics(), contains(diagnostic));
    }

    @Test
    @DisplayName("Changes to the original artifacts do not change the result")
    void copiesArtifacts() {
        var artifacts = new ArrayList<>(List.of(artifact));
        var result = CompilationResult.success(artifacts);
        artifacts.clear();
        assertThat(result.artifacts(), contains(artifact));
    }

    @Test
    @DisplayName("Returned diagnostics cannot be modified")
    void exposesUnmodifiableDiagnostics() {
        var result = CompilationResult.failure(List.of(diagnostic));
        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().removeFirst());
    }

    @Test
    @DisplayName("Returned artifacts cannot be modified")
    void exposesUnmodifiableArtifacts() {
        var result = CompilationResult.success(List.of(artifact));
        assertThrows(UnsupportedOperationException.class, () -> result.artifacts().removeFirst());
    }

    @Test
    @DisplayName("Successful compilation has artifacts and no diagnostics")
    void succeedsWithArtifacts() {
        var result = CompilationResult.success(List.of(artifact));
        assertThat(result.succeeded(), is(true));
        assertThat(result.diagnostics(), is(empty()));
        assertThat(result.artifacts(), contains(artifact));
    }

    @Test
    @DisplayName("Failed compilation has diagnostics and no artifacts")
    void failsWithDiagnostics() {
        var result = CompilationResult.failure(List.of(diagnostic));
        assertThat(result.succeeded(), is(false));
        assertThat(result.diagnostics(), contains(diagnostic));
        assertThat(result.artifacts(), is(empty()));
    }
}
