package dev.bounded.compiler.api;

import dev.bounded.compiler.api.write.OutputTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Compilation request invariants")
class CompilationRequestTest {
    private static Stream<Arguments> missingInputs() {
        return Stream.of(
                Arguments.of("source file", null, new OutputTarget(Path.of("output"))),
                Arguments.of("output target", new SourceFile("Amount.bounded", "value Amount {}"), null));
    }

    @DisplayName("Compilation requires a source and output target")
    @ParameterizedTest(name = "Rejects a missing {0}")
    @MethodSource("missingInputs")
    void rejectsMissingInputs(String description, SourceFile sourceFile, OutputTarget outputTarget) {
        assertThrows(NullPointerException.class, () -> new CompilationRequest(sourceFile, outputTarget));
    }
}
