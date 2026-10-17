package dev.bounded.compiler.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArcheClassKindTest {
    @DisplayName("Provides a human-readable label for each archeclass kind")
    @ParameterizedTest(name = "Labels {0} as {1}")
    @CsvSource({"VALUE, Value", "ENTITY, Entity"})
    void providesLabel(ArcheClassKind kind, String expectedLabel) {
        assertThat(kind.label(), is(expectedLabel));
    }

    @Test
    @DisplayName("Converts value regardless of letter case")
    void convertsValue() {
        assertThat(ArcheClassKind.fromKeyword("value"), is(ArcheClassKind.VALUE));
        assertThat(ArcheClassKind.fromKeyword("VALUE"), is(ArcheClassKind.VALUE));
        assertThat(ArcheClassKind.fromKeyword("Value"), is(ArcheClassKind.VALUE));
    }

    @Test
    @DisplayName("Converts entity regardless of letter case")
    void convertsEntity() {
        assertThat(ArcheClassKind.fromKeyword("entity"), is(ArcheClassKind.ENTITY));
        assertThat(ArcheClassKind.fromKeyword("ENTITY"), is(ArcheClassKind.ENTITY));
        assertThat(ArcheClassKind.fromKeyword("Entity"), is(ArcheClassKind.ENTITY));
    }

    @DisplayName("Rejects text that is not a keyword")
    @ParameterizedTest(name = "Rejects keyword [{0}]")
    @ValueSource(strings = {"", "function", " value "})
    void rejectsUnknownKeywords(String keyword) {
        assertThrows(IllegalArgumentException.class, () -> ArcheClassKind.fromKeyword(keyword));
    }

    @Test
    @DisplayName("Rejects a null keyword")
    void rejectsANullKeyword() {
        assertThrows(NullPointerException.class, () -> ArcheClassKind.fromKeyword(null));
    }
}
