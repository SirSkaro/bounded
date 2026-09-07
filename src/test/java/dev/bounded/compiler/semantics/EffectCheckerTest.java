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
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectCheckerTest {
    private final EffectChecker checker = new EffectChecker();

    @Test
    @DisplayName("Value archeclass can have function acheop")
    void allowsAFunctionOnAValue() {
        var archeop = new ArcheOperationDeclaration(
                ArcheOperationKind.FUNCTION,
                "someFn",
                new SourceSpan(2, 3));
        var program = givenProgram(ArcheClassKind.VALUE, archeop);

        var diagnostics = checker.check(program);
        assertThat(diagnostics, is(empty())); //is(legalProgram())
    }

    @Test
    @DisplayName("Vale archeclass cannot have a query archeop")
    void rejectsAQueryOnAValue() {
        var archeop = new ArcheOperationDeclaration(
            ArcheOperationKind.QUERY,
            "exchangeRate",
            new SourceSpan(3, 3)
        );

        var program = givenProgram(ArcheClassKind.VALUE, archeop);

        var diagnostics = checker.check(program);

        assertThat(diagnostics, hasSize(1)); //is(not(legalProgram()))
    }

    @Test
    @DisplayName("Entity archeclass can have a query archeop")
    void allowsAQueryOnAnEntity() {
        var archeop = new ArcheOperationDeclaration(
            ArcheOperationKind.QUERY,
            "balance",
            new SourceSpan(2, 3)
        );

        var program = givenProgram(ArcheClassKind.ENTITY, archeop);

        var diagnostics = checker.check(program);

        assertThat(diagnostics, is(empty())); //is(legalProgram())
    }

    private BoundedProgram givenProgram(ArcheClassKind kind, ArcheOperationDeclaration operation) {
        var declaration = new ArcheClassDeclaration(
            kind,
            "Money",
            List.of(operation),
            new SourceSpan(1, 1)
        );
        return new BoundedProgram(declaration);
    }
}
