package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.diagnostics.Diagnostic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.DUPLICATE_OPERATION;
import static dev.bounded.compiler.semantics.SemanticPrograms.operation;
import static dev.bounded.compiler.semantics.SemanticPrograms.value;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@DisplayName("Operation declaration uniqueness")
class DeclarationCheckerTest {
    private final DeclarationChecker checker = new DeclarationChecker();

    @Test
    @DisplayName("An archeclass may declare no operations")
    void acceptsNoOperations() {
        assertThat(checker.check(value()), is(empty()));
    }

    @Test
    @DisplayName("Operation names are case-sensitive")
    void acceptsDistinctNames() {
        var program = value(operation(FUNCTION, "format", 2), operation(FUNCTION, "Format", 3));
        assertThat(checker.check(program), is(empty()));
    }

    @DisplayName("The operation kind does not create a separate name scope")
    @ParameterizedTest(name = "Rejects a duplicate declared as {0}")
    @EnumSource(ArcheOperationKind.class)
    void diagnosesDuplicateAtItsDeclaration(ArcheOperationKind kind) {
        var duplicate = operation(kind, "format", 4);
        var program = value(operation(FUNCTION, "format", 2), operation(FUNCTION, "total", 3), duplicate);
        assertThat(checker.check(program), contains(new Diagnostic(DUPLICATE_OPERATION, duplicate.span(),
                "Operation 'format' is already declared in 'Money'.")));
    }

    @Test
    @DisplayName("Reports every redeclaration in source order")
    void diagnosesRepeatedDuplicates() {
        var second = operation(FUNCTION, "format", 3);
        var third = operation(FUNCTION, "format", 4);
        var result = checker.check(value(operation(FUNCTION, "format", 2), second, third));
        assertThat(result.stream().map(Diagnostic::span).toList(), contains(second.span(), third.span()));
    }

    @Test
    @DisplayName("Separate programs have independent operation scopes")
    void doesNotRetainNamesBetweenPrograms() {
        var program = value(operation(FUNCTION, "format", 2));
        checker.check(program);
        assertThat(checker.check(program), is(empty()));
    }
}
