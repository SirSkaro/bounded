package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectCheckerTest {
    private final EffectChecker checker = new EffectChecker();

    @Test
    @DisplayName("Value archeclass can have function acheop")
    void allowsAFunctionOnAValue() {
        var archeop = new ArcheOperationDeclaration(
                ArcheOperationKind.FUNCTION,
                "format",
                new SourceSpan(2, 3));
        var program = program(ArcheClassKind.VALUE, archeop);

        var diagnostics = checker.check(program);
        assertThat(diagnostics, is(empty()));
    }

    @Test
    void rejectsAQueryOnAValueWithAStableDiagnostic() {
        var program = program(
                ArcheClassKind.VALUE,
                new ArcheOperationDeclaration(
                        ArcheOperationKind.QUERY, "exchangeRate", new SourceSpan(3, 3)));

        var diagnostics = checker.check(program);

        assertEquals(1, diagnostics.size());
        assertEquals(
                "BND1001:3:3 Value 'Money' permits operations through FUNCTION; "
                        + "'exchangeRate' is QUERY.",
                diagnostics.getFirst().format());
    }

    @Test
    void allowsAQueryOnAnEntity() {
        var program = program(
                ArcheClassKind.ENTITY,
                new ArcheOperationDeclaration(
                        ArcheOperationKind.QUERY, "balance", new SourceSpan(2, 3)));

        assertTrue(checker.check(program).isEmpty());
    }

    private BoundedProgram program(ArcheClassKind kind, ArcheOperationDeclaration operation) {
        var declaration = new ArcheClassDeclaration(
            kind,
            "Money",
            List.of(operation),
            new SourceSpan(1, 1)
        );
        return new BoundedProgram(declaration);
    }
}
