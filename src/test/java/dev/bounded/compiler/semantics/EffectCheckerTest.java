package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static dev.bounded.compiler.ast.ArcheClassKind.ENTITY;
import static dev.bounded.compiler.ast.ArcheClassKind.USECASE;
import static dev.bounded.compiler.ast.ArcheClassKind.VALUE;
import static dev.bounded.compiler.ast.ArcheOperationKind.ACTION;
import static dev.bounded.compiler.ast.ArcheOperationKind.COMMAND;
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
                Arguments.of(ENTITY, QUERY),
                Arguments.of(ENTITY, COMMAND),
                Arguments.of(USECASE, FUNCTION),
                Arguments.of(USECASE, QUERY),
                Arguments.of(USECASE, COMMAND),
                Arguments.of(USECASE, ACTION)
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
                Arguments.of(VALUE, QUERY),
                Arguments.of(VALUE, COMMAND),
                Arguments.of(VALUE, ACTION),
                Arguments.of(ENTITY, ACTION)
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

    @DisplayName("An illegal effect produces a diagnostic with the correct code, span, and message")
    @ParameterizedTest(name = "Diagnoses forbidden {1} operation {2} in {0}")
    @CsvSource({
        "VALUE, QUERY, exchangeRate, Value 'Money' permits operations through FUNCTION; 'exchangeRate' is QUERY.",
        "VALUE, COMMAND, changeCurrency, Value 'Money' permits operations through FUNCTION; 'changeCurrency' is COMMAND.",
        "VALUE, ACTION, commit, Value 'Money' permits operations through FUNCTION; 'commit' is ACTION.",
        "ENTITY, ACTION, commit, Entity 'Money' permits operations through COMMAND; 'commit' is ACTION."
    })
    void constructsDiagnosticForIllegalEffect(ArcheClassKind classKind, ArcheOperationKind kind,
                                             String name, String expectedMessage) {
        var archeop = new ArcheOperationDeclaration(
            kind,
            name,
            new SourceSpan("money.bounded", 3, 7)
        );
        var program = givenProgram(classKind, archeop);

        var result = checker.check(program);
        assertThat(result, hasSize(1));

        var diagnostic = result.getFirst();
        assertThat(diagnostic.code(), is(EFFECT_NOT_ALLOWED));
        assertThat(diagnostic.span(), is(new SourceSpan("money.bounded", 3, 7)));
        assertThat(diagnostic.message(), is(expectedMessage));
    }

    @Test
    @DisplayName("All illegal effects get a corresponding diagnostic")
    void recordsDiagnosticsForMultipleIllegalOperations() {
        var firstQuery = new ArcheOperationDeclaration(
            QUERY,
            "exchangeRate",
            new SourceSpan("money.bounded", 2, 3)
        );
        var secondQuery = new ArcheOperationDeclaration(
            QUERY,
            "balance",
            new SourceSpan("money.bounded", 3, 3)
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
            new SourceSpan("money.bounded", 1, 1)
        );
        return new BoundedProgram(declaration);
    }

    private ArcheOperationDeclaration givenArcheOp(ArcheOperationKind kind) {
        return new ArcheOperationDeclaration(
            kind,
            "someOp",
            new SourceSpan("money.bounded", 2, 3)
        );
    }

}
