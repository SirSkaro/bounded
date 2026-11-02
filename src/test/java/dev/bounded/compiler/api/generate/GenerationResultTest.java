package dev.bounded.compiler.api.generate;

import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static dev.bounded.compiler.api.generate.GenerationResult.invalidResult;
import static dev.bounded.compiler.api.generate.GenerationResult.validResult;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.JVM_BACKEND_FAILURE;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Generation result invariants")
class GenerationResultTest {
    private final GeneratedClass generatedClass = new GeneratedClass("example.Amount", new byte[]{1, 2, 3});
    private final Diagnostic diagnostic = new Diagnostic(JVM_BACKEND_FAILURE, new SourceSpan(2, 3), "Cannot generate class");

    @Test
    @DisplayName("A valid result retains the generated class")
    void retainsGeneratedClass() {
        assertThat(validResult(generatedClass).generatedClass(), sameInstance(generatedClass));
    }

    @Test
    @DisplayName("A valid result has no diagnostics")
    void hasNoDiagnosticsForAValidResult() {
        assertThat(validResult(generatedClass).diagnostics(), is(empty()));
    }

    @Test
    @DisplayName("A generated class makes the result valid")
    void reportsAValidResult() {
        assertThat(validResult(generatedClass).isValid(), is(true));
    }

    @Test
    @DisplayName("A valid result requires a generated class")
    void rejectsNullGeneratedClass() {
        assertThrows(NullPointerException.class, () -> validResult(null));
    }

    @Test
    @DisplayName("An invalid result contains no generated class")
    void hasNoGeneratedClassForAnInvalidResult() {
        assertThat(invalidResult(List.of(diagnostic)).generatedClass(), is(nullValue()));
    }

    @Test
    @DisplayName("Generation diagnostics make the result invalid")
    void reportsAnInvalidResult() {
        assertThat(invalidResult(List.of(diagnostic)).isValid(), is(false));
    }

    private static Stream<Arguments> invalidDiagnostics() {
        return Stream.of(
                Arguments.of("a null diagnostics list", null, NullPointerException.class),
                Arguments.of("a null diagnostic", singletonList(null), NullPointerException.class),
                Arguments.of("no diagnostics", List.of(), IllegalArgumentException.class));
    }

    @DisplayName("An invalid result requires non-null diagnostics")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidDiagnostics")
    void rejectsInvalidDiagnostics(String description, List<Diagnostic> diagnostics,
                                  Class<? extends RuntimeException> exceptionType) {
        assertThrows(exceptionType, () -> invalidResult(diagnostics));
    }

    @Test
    @DisplayName("Diagnostics retain their supplied order")
    void preservesDiagnosticOrder() {
        var second = new Diagnostic(JVM_BACKEND_FAILURE, new SourceSpan(3, 1), "Another generation error");

        assertThat(invalidResult(List.of(diagnostic, second)).diagnostics(), contains(diagnostic, second));
    }

    @Test
    @DisplayName("Changes to the original diagnostics cannot change the result")
    void copiesDiagnostics() {
        var diagnostics = new ArrayList<>(List.of(diagnostic));
        var result = invalidResult(diagnostics);

        diagnostics.clear();

        assertThat(result.diagnostics(), contains(diagnostic));
    }

    @Test
    @DisplayName("Returned diagnostics cannot be modified")
    void exposesImmutableDiagnostics() {
        var result = invalidResult(List.of(diagnostic));

        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().removeFirst());
    }
}
