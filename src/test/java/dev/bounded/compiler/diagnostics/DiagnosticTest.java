package dev.bounded.compiler.diagnostics;

import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DiagnosticTest {

    @ParameterizedTest(name = "Rejects null {0}")
    @MethodSource("nullConstructorArguments")
    @DisplayName("Constructor rejects null arguments")
    void rejectsNullConstructorArguments(
        String nullArguments,
        DiagnosticCode code,
        SourceSpan span,
        String message
    ) {
        assertThrows(NullPointerException.class, () -> new Diagnostic(code, span, message));
    }

    private static Stream<Arguments> nullConstructorArguments() {
        var code = DiagnosticCode.SYNTAX_ERROR;
        var span = new SourceSpan(1, 2);
        var message = "Unexpected token.";

        return Stream.of(
            Arguments.of("code", null, span, message),
            Arguments.of("span", code, null, message),
            Arguments.of("message", code, span, null)
        );
    }
}
