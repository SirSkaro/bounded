package dev.bounded.compiler.api.write;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Write result invariants")
class WriteResultTest {
    private final Path classFile = Path.of("Amount.class");

    private static Stream<Arguments> invalidClassFiles() {
        return Stream.of(
                Arguments.of("a null list", null),
                Arguments.of("a null class file", singletonList(null)));
    }

    @DisplayName("Class file paths must be non-null")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidClassFiles")
    void rejectsNullClassFiles(String description, List<Path> classFiles) {
        assertThrows(NullPointerException.class, () -> new WriteResult(classFiles));
    }

    @Test
    @DisplayName("Class file paths retain their supplied order")
    void preservesClassFileOrder() {
        var second = Path.of("AmountHelper.class");

        assertThat(new WriteResult(List.of(classFile, second)).generatedClassFiles(), contains(classFile, second));
    }

    @Test
    @DisplayName("Changes to the original paths do not change the result")
    void copiesClassFiles() {
        var files = new ArrayList<>(List.of(classFile));
        var result = new WriteResult(files);

        files.clear();

        assertThat(result.generatedClassFiles(), contains(classFile));
    }

    @Test
    @DisplayName("Returned class file paths cannot be modified")
    void exposesImmutableClassFiles() {
        var result = new WriteResult(List.of(classFile));

        assertThrows(UnsupportedOperationException.class, () -> result.generatedClassFiles().removeFirst());
    }

    @Test
    @DisplayName("A writer may report no generated class files")
    void acceptsAnEmptyResult() {
        assertThat(new WriteResult(List.of()).generatedClassFiles(), is(empty()));
    }
}
