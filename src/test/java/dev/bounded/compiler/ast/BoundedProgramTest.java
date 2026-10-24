package dev.bounded.compiler.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.bounded.compiler.ast.ArcheClassKind.VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedProgramTest {
    @Test
    @DisplayName("Requires an archeclass")
    void rejectsNullArcheClass() {
        assertThrows(NullPointerException.class, () -> new BoundedProgram(null));
    }

    @Test
    @DisplayName("Has valid representation")
    void retainsArcheClass() {
        var archeClass = new ArcheClassDeclaration(VALUE, "Amount", List.of(), new SourceSpan(1, 1));

        var program = new BoundedProgram(archeClass);

        assertThat(program.archeClass(), sameInstance(archeClass));
    }
}
