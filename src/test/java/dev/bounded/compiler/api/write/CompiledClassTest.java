package dev.bounded.compiler.api.write;

import dev.bounded.compiler.api.generate.ClassName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Loadable class descriptors")
class CompiledClassTest {
    private static Stream<Arguments> missingFields() {
        return Stream.of(Arguments.of("class name", null, Path.of("output")),
                Arguments.of("class path root", new ClassName("Amount"), null));
    }

    @DisplayName("Requires both a name and a class path root")
    @ParameterizedTest(name = "Rejects a missing {0}")
    @MethodSource("missingFields")
    void rejectsMissingFields(String description, ClassName name, Path root) {
        assertThrows(NullPointerException.class, () -> new CompiledClass(name, root));
    }

    @Test
    @DisplayName("Locates its file using its own name and class path root")
    void locatesClassFile() {
        var name = new ClassName("example.Amount");
        var root = Path.of("output/classes");
        var artifact = new CompiledClass(name, root);
        assertThat(artifact.className(), is(name));
        assertThat(artifact.classPathRoot(), is(root));
        assertThat(artifact.classFile(), is(root.resolve("example/Amount.class")));
    }
}
