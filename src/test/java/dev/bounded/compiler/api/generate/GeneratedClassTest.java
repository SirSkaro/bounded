package dev.bounded.compiler.api.generate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratedClassTest {

    private static Stream<Arguments> invalidConstructorArguments() {
        return Stream.of(
                Arguments.of("null class name", null, new byte[]{1, 2, 3}),
                Arguments.of("null bytecode", "bounded.generated.Bounded_Amount", null)
        );
    }
    @DisplayName("Rejects null constructor inputs")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidConstructorArguments")
    void rejectsNullInputs(String description, String className, byte[] bytecode) {
        assertThrows(NullPointerException.class, () -> new GeneratedClass(className, bytecode));
    }

    @Test
    @DisplayName("Changes to the original bytecode cannot change the generated class")
    void copiesBytecode() {
        var bytecode = new byte[]{1, 2, 3};
        var generatedClass = new GeneratedClass("bounded.generated.Bounded_Amount", bytecode);

        bytecode[0] = 4;

        assertThat(generatedClass.bytecode(), is(new byte[]{1, 2, 3}));
    }

    @Test
    @DisplayName("Changes to returned bytecode cannot change the generated class")
    void protectsReturnedBytecode() {
        var generatedClass = new GeneratedClass("bounded.generated.Bounded_Amount", new byte[]{1, 2, 3});

        generatedClass.bytecode()[0] = 4;

        assertThat(generatedClass.bytecode(), is(new byte[]{1, 2, 3}));
    }
}
