package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static dev.bounded.compiler.ast.ArcheClassKind.ENTITY;
import static dev.bounded.compiler.ast.ArcheClassKind.VALUE;
import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static dev.bounded.compiler.ast.ArcheOperationKind.QUERY;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.EFFECT_NOT_ALLOWED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class EffectCheckerTest {
    private final EffectChecker checker = new EffectChecker();

    @Nested
    @DisplayName("Allowed archeclass and archeop combinations")
    class AllowedOperations {
        static Stream<Arguments> allowedCombinations() {
            return Stream.of(
                Arguments.of(VALUE, FUNCTION),
                Arguments.of(ENTITY, FUNCTION),
                Arguments.of(ENTITY, QUERY)
            );
        }
        @ParameterizedTest(name = "{0} archeclass can have a {1} archeop")
        @MethodSource("allowedCombinations")
        void allowsLegalOperation(ArcheClassKind archeClassKind, ArcheOperationKind operationKind) {
            var archeop = givenArcheOp(operationKind);
            var program = givenProgram(archeClassKind, archeop);

            var result = checker.check(program);
            assertThat(result, is(empty()));
        }
    }

    @Nested
    @DisplayName("Disallowed archeclass and archeop combinations")
    class DisallowedOperations {
        static Stream<Arguments> disallowedCombinations() {
            return Stream.of(
                Arguments.of(VALUE, QUERY)
            );
        }
        @ParameterizedTest(name = "{0} archeclass cannot have a {1} archeop")
        @MethodSource("disallowedCombinations")
        void rejectsIllegalOperation(ArcheClassKind archeClassKind, ArcheOperationKind operationKind) {
            var archeop = givenArcheOp(operationKind);
            var program = givenProgram(archeClassKind, archeop);

            var result = checker.check(program);

            assertThat(result, is(not(empty())));
            assertThat(result, hasSize(1));
        }
    }

    @Test
    @DisplayName("An illegal effect produces a diagnostic with the correct code, span, and message")
    void constructsDiagnosticForIllegalEffect() {
        var archeop = new ArcheOperationDeclaration(
            QUERY,
            "exchangeRate",
            new SourceSpan(3, 7)
        );
        var program = givenProgram(VALUE, archeop);

        var result = checker.check(program);
        assertThat(result, hasSize(1));

        var diagnostic = result.getFirst();
        assertThat(diagnostic.code(), is(EFFECT_NOT_ALLOWED));
        assertThat(diagnostic.span().line(), is(3));
        assertThat(diagnostic.span().column(), is(7));
        assertThat(diagnostic.message(), is(equalTo(
    "Value 'Money' permits operations through FUNCTION; 'exchangeRate' is QUERY."
        )));
    }

    @Test
    @DisplayName("All illegal effects get a corresponding diagnostic")
    void recordsDiagnosticsForMultipleIllegalOperations() {
        var firstQuery = new ArcheOperationDeclaration(
            QUERY,
            "exchangeRate",
            new SourceSpan(2, 3)
        );
        var secondQuery = new ArcheOperationDeclaration(
            QUERY,
            "balance",
            new SourceSpan(3, 3)
        );
        var program = givenProgram(VALUE, firstQuery, secondQuery);

        var result = checker.check(program);

        assertThat(result, is(not(empty())));
        assertThat(result, hasSize(2));
        assertThat(result.get(0).span(), is(firstQuery.span()));
        assertThat(result.get(1).span(), is(secondQuery.span()));
    }

    private BoundedProgram givenProgram(ArcheClassKind kind, ArcheOperationDeclaration ...operations) {
        var declaration = new ArcheClassDeclaration(
            kind,
            "Money",
            Arrays.asList(operations),
            new SourceSpan(1, 1)
        );
        return new BoundedProgram(declaration);
    }

    private ArcheOperationDeclaration givenArcheOp(ArcheOperationKind kind) {
        return new ArcheOperationDeclaration(
            kind,
            "someOp",
            new SourceSpan(2, 3)
        );
    }

}
