package dev.bounded.compiler.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceSpanTest {
    @Test
    @DisplayName("Preserves the source identity independently of position")
    void preservesSourceName() {
        assertThat(new SourceSpan("lib/money.bounded", 3, 7).sourceName(), is("lib/money.bounded"));
    }

    @Test
    @DisplayName("Positions without a known source use an explicit unknown marker")
    void identifiesUnknownSource() {
        assertThat(new SourceSpan(3, 7).sourceName(), is("<unknown>"));
    }

    @Test
    @DisplayName("Requires a non-null source identity")
    void rejectsNullSourceName() {
        assertThrows(NullPointerException.class, () -> new SourceSpan(null, 3, 7));
    }

    @DisplayName("Preserves valid one-based source positions")
    @ParameterizedTest(name = "Preserves line {0}, column {1}")
    @CsvSource({
            "1, 1",
            "1, 42",
            "27, 1",
            "27, 42",
            "2147483647, 2147483647"
    })
    void preservesValidPositions(int line, int column) {
        var span = new SourceSpan(line, column);

        assertThat(span.line(), is(line));
        assertThat(span.column(), is(column));
    }

    @DisplayName("Rejects lines below one")
    @ParameterizedTest(name = "Rejects line {0} with a valid column")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsInvalidLines(int line) {
        assertThrows(IllegalArgumentException.class, () -> new SourceSpan(line, 1));
    }

    @DisplayName("Rejects columns below one")
    @ParameterizedTest(name = "Rejects column {0} with a valid line")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejectsInvalidColumns(int column) {
        assertThrows(IllegalArgumentException.class, () -> new SourceSpan(1, column));
    }
}
