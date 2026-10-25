package dev.bounded.compiler.api;

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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompilationResultTest {
    private final Diagnostic diagnostic = new Diagnostic(SYNTAX_ERROR, new SourceSpan(1, 1), "Invalid syntax");
    private final Path sourceFile = Path.of("Amount.java");
    private final Path classFile = Path.of("Amount.class");

    private static Stream<Arguments> invalidConstructorArguments() {
        return Stream.of(
                Arguments.of("null diagnostics", null, List.of(), List.of()),
                Arguments.of("null generated source files", List.of(), null, List.of()),
                Arguments.of("null generated class files", List.of(), List.of(), null),
                Arguments.of("null diagnostic entry", singletonList(null), List.of(), List.of()),
                Arguments.of("null source file entry", List.of(), singletonList(null), List.of()),
                Arguments.of("null class file entry", List.of(), List.of(), singletonList(null))
        );
    }
    @DisplayName("Rejects null collections and entries")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidConstructorArguments")
    void rejectsNullInputs(String description, List<Diagnostic> diagnostics,
                           List<Path> sourceFiles, List<Path> classFiles) {
        assertThrows(NullPointerException.class, () -> new CompilationResult(diagnostics, sourceFiles, classFiles));
    }

    @Test
    @DisplayName("Changes to the original diagnostics do not change the result")
    void copiesDiagnostics() {
        var diagnostics = new ArrayList<>(List.of(diagnostic));
        var result = new CompilationResult(diagnostics, List.of(), List.of());

        diagnostics.clear();

        assertThat(result.diagnostics(), contains(diagnostic));
    }

    @Test
    @DisplayName("Changes to the original source files do not change the result")
    void copiesSourceFiles() {
        var files = new ArrayList<>(List.of(sourceFile));
        var result = new CompilationResult(List.of(), files, List.of());

        files.clear();

        assertThat(result.generatedSourceFiles(), contains(sourceFile));
    }

    @Test
    @DisplayName("Changes to the original class files do not change the result")
    void copiesClassFiles() {
        var files = new ArrayList<>(List.of(classFile));
        var result = new CompilationResult(List.of(), List.of(), files);

        files.clear();

        assertThat(result.generatedClassFiles(), contains(classFile));
    }

    @Test
    @DisplayName("Returned diagnostics cannot be modified")
    void exposesUnmodifiableDiagnostics() {
        var result = new CompilationResult(List.of(diagnostic), List.of(), List.of());

        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().removeFirst());
    }

    @Test
    @DisplayName("Returned source files cannot be modified")
    void exposesUnmodifiableSourceFiles() {
        var result = new CompilationResult(List.of(), List.of(sourceFile), List.of());

        assertThrows(UnsupportedOperationException.class, () -> result.generatedSourceFiles().removeFirst());
    }

    @Test
    @DisplayName("Returned class files cannot be modified")
    void exposesUnmodifiableClassFiles() {
        var result = new CompilationResult(List.of(), List.of(), List.of(classFile));

        assertThrows(UnsupportedOperationException.class, () -> result.generatedClassFiles().removeFirst());
    }

    @Test
    @DisplayName("Compilation succeeds without diagnostics or generated files")
    void succeedsWithoutDiagnostics() {
        var result = new CompilationResult(List.of(), List.of(), List.of());

        assertThat(result.succeeded(), is(true));
    }

    @Test
    @DisplayName("Diagnostics indicate failure even when files were generated")
    void failsWithDiagnostics() {
        var result = new CompilationResult(List.of(diagnostic), List.of(sourceFile), List.of(classFile));

        assertThat(result.succeeded(), is(false));
    }
}
