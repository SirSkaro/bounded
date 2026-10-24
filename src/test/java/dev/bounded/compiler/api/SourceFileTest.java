package dev.bounded.compiler.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SourceFileTest {
    private static Stream<Arguments> invalidConstructorArguments() {
        return Stream.of(
                Arguments.of("null name", null, "value Amount {}"),
                Arguments.of("null content", "Amount.bounded", null)
        );
    }

    @DisplayName("Rejects null constructor inputs")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidConstructorArguments")
    void rejectsNullInputs(String description, String name, String content) {
        assertThrows(NullPointerException.class, () -> new SourceFile(name, content));
    }

    @DisplayName("Preserves source content exactly, including empty input")
    @ParameterizedTest(name = "Preserves source content [{index}]")
    @EmptySource
    @ValueSource(strings = {"value Amount {}", " \t\r\n"})
    void preservesContent(String content) {
        var source = new SourceFile("Amount.bounded", content);

        assertThat(source.content(), is(content));
    }

    @DisplayName("Preserves the supplied source name")
    @ParameterizedTest(name = "Preserves source name {0}")
    @ValueSource(strings = {"Amount.bounded", "src/Amount.bounded"})
    void preservesName(String name) {
        var source = new SourceFile(name, "");

        assertThat(source.name(), is(name));
    }
}
