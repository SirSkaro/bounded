package dev.bounded.compiler.diagnostics;

import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DisplayName("Text diagnostic formatting")
class TextDiagnosticFormatterTest {
    @DisplayName("Formats source:code:line:column message without locale-dependent numbers")
    @ParameterizedTest(name = "Preserves location {0}:{1}:{2}")
    @CsvSource({"money.bounded, 3, 7", "folder/account.bounded, 1234, 5678"})
    void formatsAnError(String sourceName, int line, int column) {
        var diagnostic = new Diagnostic(DiagnosticCode.EFFECT_NOT_ALLOWED,
                new SourceSpan(sourceName, line, column), "Query is not allowed on a value.");

        assertThat(TextDiagnosticFormatter.format(diagnostic), is(sourceName + ":BND1001:" + line + ":" + column
                + " Query is not allowed on a value."));
    }
}
