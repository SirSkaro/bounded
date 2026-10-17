package dev.bounded.compiler.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArcheOperationKindTest {
    @DisplayName("Provides a human-readable label for each archeoperation kind")
    @ParameterizedTest(name = "Labels {0} as {1}")
    @CsvSource({"FUNCTION, Function", "QUERY, Query"})
    void providesLabel(ArcheOperationKind kind, String expectedLabel) {
        assertThat(kind.label(), is(expectedLabel));
    }

    @Test
    @DisplayName("Converts function regardless of letter case")
    void convertsFunction() {
        assertThat(ArcheOperationKind.fromKeyword("function"), is(ArcheOperationKind.FUNCTION));
        assertThat(ArcheOperationKind.fromKeyword("FUNCTION"), is(ArcheOperationKind.FUNCTION));
        assertThat(ArcheOperationKind.fromKeyword("Function"), is(ArcheOperationKind.FUNCTION));
    }

    @Test
    @DisplayName("Converts query regardless of letter case")
    void convertsQuery() {
        assertThat(ArcheOperationKind.fromKeyword("query"), is(ArcheOperationKind.QUERY));
        assertThat(ArcheOperationKind.fromKeyword("QUERY"), is(ArcheOperationKind.QUERY));
        assertThat(ArcheOperationKind.fromKeyword("Query"), is(ArcheOperationKind.QUERY));
    }

    @DisplayName("Rejects text that is not a keyword")
    @ParameterizedTest(name = "Rejects keyword [{0}]")
    @EmptySource
    @ValueSource(strings = {"entity", " function "})
    void rejectsUnknownKeywords(String keyword) {
        assertThrows(IllegalArgumentException.class, () -> ArcheOperationKind.fromKeyword(keyword));
    }

    @Test
    @DisplayName("Rejects a null keyword")
    void rejectsANullKeyword() {
        assertThrows(NullPointerException.class, () -> ArcheOperationKind.fromKeyword(null));
    }
}
