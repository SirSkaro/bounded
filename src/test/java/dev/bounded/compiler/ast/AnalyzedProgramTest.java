package dev.bounded.compiler.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.bounded.compiler.ast.ArcheClassKind.VALUE;
import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Retained semantic analysis")
class AnalyzedProgramTest {
    private final SourceSpan span = new SourceSpan("money.bounded", 1, 1);

    @Test
    @DisplayName("Retains the parsed program")
    void retainsProgram() {
        var program = program(List.of());
        assertThat(new AnalyzedProgram(program).program(), sameInstance(program));
    }

    @Test
    @DisplayName("Indexes declarations in source order")
    void indexesOperations() {
        var first = new ArcheOperationDeclaration(FUNCTION, "zeta", span);
        var second = new ArcheOperationDeclaration(FUNCTION, "alpha", span);
        var analysis = new AnalyzedProgram(program(List.of(first, second)));
        assertThat(analysis.operations().keySet(), contains("zeta", "alpha"));
        assertThat(analysis.operations().values(), contains(first, second));
    }

    @Test
    @DisplayName("Empty archeclasses have an empty declaration index")
    void indexesEmptyProgram() {
        assertThat(new AnalyzedProgram(program(List.of())).operations(), is(anEmptyMap()));
    }

    @Test
    @DisplayName("The declaration index cannot be changed")
    void exposesImmutableIndex() {
        var declaration = new ArcheOperationDeclaration(FUNCTION, "format", span);
        var analysis = new AnalyzedProgram(program(List.of(declaration)));
        assertThrows(UnsupportedOperationException.class, () -> analysis.operations().remove("format"));
    }

    @Test
    @DisplayName("An analyzed program cannot contain ambiguous operation names")
    void rejectsDuplicateNames() {
        var declaration = new ArcheOperationDeclaration(FUNCTION, "format", span);
        assertThrows(IllegalArgumentException.class,
                () -> new AnalyzedProgram(program(List.of(declaration, declaration))));
    }

    @Test
    @DisplayName("Analysis requires a parsed program")
    void rejectsMissingProgram() {
        assertThrows(NullPointerException.class, () -> new AnalyzedProgram(null));
    }

    private BoundedProgram program(List<ArcheOperationDeclaration> operations) {
        return new BoundedProgram(new ArcheClassDeclaration(VALUE, "Money", operations, span));
    }
}
