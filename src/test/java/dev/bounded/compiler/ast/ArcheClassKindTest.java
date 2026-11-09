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
    @DisplayName("Provides stable metadata independent of the Java enum name")
    @ParameterizedTest(name = "{0} exposes metadata {1}")
    @CsvSource({"VALUE, VALUE", "ENTITY, ENTITY", "USECASE, USECASE"})
    void providesMetadataName(ArcheClassKind kind, String expected) {
        assertThat(kind.metadataName(), is(expected));
    }

    @DisplayName("Determines which operation kinds an archeclass may declare")
    @ParameterizedTest(name = "{0} allows {1}: {2}")
    @CsvSource({
        "VALUE, FUNCTION, true",
        "VALUE, QUERY, false",
        "VALUE, COMMAND, false",
        "VALUE, ACTION, false",
        "ENTITY, FUNCTION, true",
        "ENTITY, QUERY, true",
        "ENTITY, COMMAND, true",
        "ENTITY, ACTION, false",
        "USECASE, FUNCTION, true",
        "USECASE, QUERY, true",
        "USECASE, COMMAND, true",
        "USECASE, ACTION, true"
    })
    void allowsOperationKinds(ArcheClassKind classKind, ArcheOperationKind operationKind, boolean expected) {
        assertThat(classKind.allows(operationKind), is(expected));
    }

    @DisplayName("Provides a human-readable label for each archeclass kind")
    @ParameterizedTest(name = "Labels {0} as {1}")
    @CsvSource({"VALUE, Value", "ENTITY, Entity", "USECASE, Use case"})
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

    @Test
    @DisplayName("Converts usecase regardless of letter case")
    void convertsUseCase() {
        assertThat(ArcheClassKind.fromKeyword("usecase"), is(ArcheClassKind.USECASE));
        assertThat(ArcheClassKind.fromKeyword("USECASE"), is(ArcheClassKind.USECASE));
        assertThat(ArcheClassKind.fromKeyword("UseCase"), is(ArcheClassKind.USECASE));
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
