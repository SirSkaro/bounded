package dev.bounded.compiler.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static dev.bounded.compiler.ast.ArcheClassKind.VALUE;
import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArcheClassDeclarationTest {
    private final SourceSpan span = new SourceSpan(1, 1);
    private final ArcheOperationDeclaration firstOperation =
            new ArcheOperationDeclaration(FUNCTION, "first", new SourceSpan(2, 1));
    private final ArcheOperationDeclaration secondOperation =
            new ArcheOperationDeclaration(FUNCTION, "second", new SourceSpan(3, 1));

    private static Stream<Arguments> invalidConstructorArguments() {
        var span = new SourceSpan(1, 1);
        var operation = new ArcheOperationDeclaration(FUNCTION, "first", new SourceSpan(2, 1));

        return Stream.of(
                Arguments.of("null kind", null, "Amount", List.of(), span),
                Arguments.of("null name", VALUE, null, List.of(), span),
                Arguments.of("null operations list", VALUE, "Amount", null, span),
                Arguments.of("null operation entry", VALUE, "Amount", Arrays.asList(operation, null), span),
                Arguments.of("null source span", VALUE, "Amount", List.of(), null)
        );
    }
    @DisplayName("Rejects null constructor inputs")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidConstructorArguments")
    void rejectsNullInputs(String description, ArcheClassKind kind, String name,
                           List<ArcheOperationDeclaration> operations, SourceSpan span) {
        assertThrows(NullPointerException.class, () -> new ArcheClassDeclaration(kind, name, operations, span));
    }

    @DisplayName("Rejects blank names")
    @ParameterizedTest(name = "Rejects blank name [{index}]")
    @EmptySource
    @ValueSource(strings = {" ", "   ", "\t", "\n", " \t\r\n", "\u2003"})
    void rejectsBlankNames(String name) {
        assertThrows(IllegalArgumentException.class, () -> new ArcheClassDeclaration(VALUE, name, List.of(), span));
    }

    @Test
    @DisplayName("Preserves operation declaration order")
    void preservesOperationOrder() {
        var declaration = new ArcheClassDeclaration(
                VALUE,
                "Amount",
                List.of(firstOperation, secondOperation),
                span
        );

        assertThat(declaration.operations(), contains(firstOperation, secondOperation));
    }

    @Test
    @DisplayName("Changes to the original list cannot change declared operations")
    void copiesOperations() {
        var operations = new ArrayList<>(List.of(firstOperation));
        var declaration = new ArcheClassDeclaration(
                VALUE,
                "Amount",
                operations,
                span
        );

        operations.clear();
        operations.add(secondOperation);

        assertThat(declaration.operations(), contains(firstOperation));
    }

    @Test
    @DisplayName("Declared operations cannot be modified through the accessor")
    void exposesUnmodifiableOperations() {
        var declaration = new ArcheClassDeclaration(
                VALUE,
                "Amount",
                List.of(firstOperation),
                span
        );

        assertThrows(UnsupportedOperationException.class, () -> declaration.operations().set(0, secondOperation));
    }
}
