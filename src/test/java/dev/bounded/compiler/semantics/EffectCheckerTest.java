package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.DiagnosticCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.EFFECT_NOT_ALLOWED;
import static dev.bounded.compiler.semantics.ValidEffectCheckMatcher.valid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

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

        var result = checker.check(program);
        assertThat(result, is(valid()));
    }

    @Test
    @DisplayName("Vale archeclass cannot have a query archeop")
    void rejectsAQueryOnAValue() {
        var archeop = new ArcheOperationDeclaration(
            ArcheOperationKind.QUERY,
            "someQuery",
            new SourceSpan(3, 3)
        );

        var program = givenProgram(ArcheClassKind.VALUE, archeop);

        var result = checker.check(program);

        assertThat(result, is(not(valid())));
        assertThat(result.diagnostics(), hasSize(1));
    }

    @Test
    @DisplayName("Entity archeclass can have a query archeop")
    void allowsAQueryOnAnEntity() {
        var archeop = new ArcheOperationDeclaration(
            ArcheOperationKind.QUERY,
            "someQuery",
            new SourceSpan(2, 3)
        );

        var program = givenProgram(ArcheClassKind.ENTITY, archeop);

        var result = checker.check(program);

        assertThat(result, is(valid()));
    }

    @Test
    @DisplayName("An illegal effect produces a diagnostic with the correct code, span, and message")
    void constructsDiagnosticForIllegalEffect() {
        var archeop = new ArcheOperationDeclaration(
            ArcheOperationKind.QUERY,
            "exchangeRate",
            new SourceSpan(3, 7)
        );
        var program = givenProgram(ArcheClassKind.VALUE, archeop);

        var result = checker.check(program);

        assertThat(result.diagnostics(), hasSize(1));
        var diagnostic = result.diagnostics().getFirst();

        assertThat(diagnostic.code(), is(EFFECT_NOT_ALLOWED));
        assertThat(diagnostic.span().line(), is(3));
        assertThat(diagnostic.span().column(), is(7));
        assertThat(diagnostic.message(), is(equalTo(
            "Value 'Money' permits operations through FUNCTION; 'exchangeRate' is QUERY."
        )));
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
