package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.SYNTAX_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyntaxErrorCollectorTest {
    SyntaxErrorCollector collector;

    @BeforeEach
    void setUp() {
        collector = new SyntaxErrorCollector();
    }

    @Test
    @DisplayName("The returned diagnostics cannot be modified and the collector retains its diagnostics")
    void exposesImmutableDiagnostics() {
        givenSyntaxError(2, 3, "Unexpected token.");
        var diagnostics = collector.diagnostics();

        assertThrows(UnsupportedOperationException.class, () -> diagnostics.add(null));
        assertThrows(UnsupportedOperationException.class, () -> diagnostics.set(0, null));
        assertThrows(UnsupportedOperationException.class, diagnostics::removeFirst);
    }

    @Test
    @DisplayName("A collected error has the syntax error diagnostic code")
    void recordsSyntaxErrorCode() {
        givenSyntaxError(2, 3, "Unexpected token.");

        var code = collector.diagnostics().getFirst().code();

        assertThat(code, is(SYNTAX_ERROR));
    }

    @ParameterizedTest(name = "Line {0}, character position {1} becomes line {0}, column {2}")
    @CsvSource({
        "1, 0, 1",
        "3, 6, 7"
    })
    @DisplayName("Source spans preserve the line and convert the column to one-based")
    void recordsSourceSpan(int line, int characterPosition, int expectedColumn) {
        givenSyntaxError(line, characterPosition, "Unexpected token.");

        var span = collector.diagnostics().getFirst().span();
        assertThat(span.line(), is(line));
        assertThat(span.column(), is(expectedColumn));
    }

    @Test
    @DisplayName("The original syntax error message is preserved")
    void preservesMessage() {
        var expectedMessage = "  mismatched input '}' expecting ';'\n";
        givenSyntaxError(2, 3, expectedMessage);

        var actualMessage = collector.diagnostics().getFirst().message();
        assertThat(actualMessage, is(equalTo(expectedMessage)));
    }

    @Test
    @DisplayName("Multiple syntax errors produce diagnostics in the order received")
    void recordsMultipleDiagnostics() {
        givenSyntaxError(2, 3, "First error.");
        givenSyntaxError(4, 0, "Second error.");

        assertThat(collector.diagnostics(), contains(
            new Diagnostic(SYNTAX_ERROR, new SourceSpan(2, 4), "First error."),
            new Diagnostic(SYNTAX_ERROR, new SourceSpan(4, 1), "Second error.")
        ));
    }

    @Test
    @DisplayName("Previously returned diagnostics do not change when another error is collected")
    void returnsSnapshotOfDiagnostics() {
        givenSyntaxError(2, 3, "First error.");
        var snapshot = collector.diagnostics();

        givenSyntaxError(4, 0, "Second error.");

        assertThat(snapshot, contains(
            new Diagnostic(SYNTAX_ERROR, new SourceSpan(2, 4), "First error.")
        ));
        assertThat(collector.diagnostics(), hasSize(2));
    }

    void givenSyntaxError(int line, int charPositionInLine, String message) {
        collector.syntaxError(null, null, line, charPositionInLine, message, null);
    }
}
