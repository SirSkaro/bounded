package dev.bounded.compiler.api.generate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Generated program invariants")
class GeneratedProgramTest {
    private final GeneratedClass primary = new GeneratedClass("example.Amount", new byte[]{1});
    private final GeneratedClass helper = new GeneratedClass("example.Helper", new byte[]{2});

    private static Stream<Arguments> invalidClasses() {
        var first = new GeneratedClass("Amount", new byte[]{1});
        var duplicate = new GeneratedClass("Amount", new byte[]{2});
        return Stream.of(
                Arguments.of("a null list", null, NullPointerException.class),
                Arguments.of("a null class", singletonList(null), NullPointerException.class),
                Arguments.of("no classes", List.of(), IllegalArgumentException.class),
                Arguments.of("duplicate binary names", List.of(first, duplicate), IllegalArgumentException.class));
    }

    @DisplayName("Requires at least one class with a unique binary name")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidClasses")
    void rejectsInvalidClasses(String description, List<GeneratedClass> classes,
                               Class<? extends RuntimeException> exceptionType) {
        assertThrows(exceptionType, () -> new GeneratedProgram(classes));
    }

    @Test
    @DisplayName("Retains all classes in supplied order independently of the caller's list")
    void copiesClassesInOrder() {
        var classes = new ArrayList<>(List.of(primary, helper));
        var program = new GeneratedProgram(classes);
        classes.clear();
        assertThat(program.classes(), contains(primary, helper));
    }

    @Test
    @DisplayName("Generated classes cannot be removed through the result")
    void exposesImmutableClasses() {
        var program = new GeneratedProgram(List.of(primary));
        assertThrows(UnsupportedOperationException.class, () -> program.classes().removeFirst());
    }
}
