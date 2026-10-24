package dev.bounded.compiler.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArcheOperationDeclarationTest {
    private static Stream<Arguments> invalidConstructorArguments() {
        var span = new SourceSpan(1, 1);

        return Stream.of(
                Arguments.of("null kind", null, "calculate", span),
                Arguments.of("null name", FUNCTION, null, span),
                Arguments.of("null source span", FUNCTION, "calculate", null)
        );
    }
    @DisplayName("Rejects null constructor inputs")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidConstructorArguments")
    void rejectsNullInputs(String description, ArcheOperationKind kind, String name, SourceSpan span) {
        assertThrows(NullPointerException.class, () -> new ArcheOperationDeclaration(kind, name, span));
    }

    @DisplayName("Rejects blank names")
    @ParameterizedTest(name = "Rejects blank name [{index}]")
    @EmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", " \t\r\n", "\u2003"})
    void rejectsBlankNames(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> new ArcheOperationDeclaration(FUNCTION, name, new SourceSpan(1, 1)));
    }

    @Test
    @DisplayName("Has valid representation")
    void valid() {
        var declaration = new ArcheOperationDeclaration(FUNCTION, "foo", new SourceSpan(1, 1));

        assertThat(declaration.name(), is("foo"));
    }
}
