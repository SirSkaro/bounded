package dev.bounded.compiler.api.generate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Binary class names")
class ClassNameTest {
    @DisplayName("Converts valid binary names to class resources")
    @ParameterizedTest(name = "{0} maps to {1}")
    @CsvSource({"Amount, Amount.class", "example.Amount, example/Amount.class",
            "a.b.Outer$Inner, a/b/Outer$Inner.class", "_a.$B2, _a/$B2.class", "exämple.État, exämple/État.class"})
    void locatesClassResource(String name, String resource) {
        var className = new ClassName(name);
        assertThat(className.binaryName(), is(name));
        assertThat(className.resourceName(), is(resource));
    }

    @DisplayName("Rejects names that are not binary class names")
    @ParameterizedTest(name = "Rejects invalid name [{0}]")
    @ValueSource(strings = {"", " ", ".Amount", "example.", "a..Amount", "1Amount", "a.2B",
            "../Amount", "/tmp/Amount", "a/Amount", "a\\Amount", "C:Amount", "A B", "A-B", "[LAmount;", "A\u0000"})
    void rejectsInvalidNames(String name) {
        assertThrows(IllegalArgumentException.class, () -> new ClassName(name));
    }

    @Test
    @DisplayName("Requires a binary name")
    void rejectsNullName() {
        assertThrows(NullPointerException.class, () -> new ClassName(null));
    }
}
