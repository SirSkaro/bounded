package dev.bounded.compiler.semantics;

import dev.bounded.compiler.diagnostics.Diagnostic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static dev.bounded.compiler.ast.ArcheOperationKind.QUERY;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.DUPLICATE_OPERATION;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.EFFECT_NOT_ALLOWED;
import static dev.bounded.compiler.semantics.SemanticPrograms.operation;
import static dev.bounded.compiler.semantics.SemanticPrograms.value;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Composed semantic analysis")
class ProgramCheckerTest {
    @Test
    @DisplayName("Successful analysis retains the parsed program and its declaration index")
    void retainsAnalyzedProgram() {
        var declaration = operation(FUNCTION, "format", 2);
        var program = value(declaration);
        var result = new ProgramChecker().check(program);
        assertThat(result.isValid(), is(true));
        assertThat(result.diagnostics(), is(empty()));
        assertThat(result.program().program(), sameInstance(program));
        assertThat(result.program().operations().get("format"), sameInstance(declaration));
    }

    @Test
    @DisplayName("Reports declaration and effect failures before producing an analyzed program")
    void appliesAllProductionRules() {
        var program = value(operation(FUNCTION, "format", 2), operation(QUERY, "format", 3));
        var result = new ProgramChecker().check(program);
        assertThat(result.isValid(), is(false));
        assertThat(result.program(), is(nullValue()));
        assertThat(result.diagnostics().stream().map(Diagnostic::code).toList(),
                contains(DUPLICATE_OPERATION, EFFECT_NOT_ALLOWED));
    }

    @Test
    @DisplayName("Composes rule diagnostics in registration order using the same parsed program")
    void composesRulesInOrder() {
        var program = value();
        var first = new Diagnostic(DUPLICATE_OPERATION, program.archeClass().span(), "First");
        var second = new Diagnostic(EFFECT_NOT_ALLOWED, program.archeClass().span(), "Second");
        SemanticRule firstRule = parsed -> {
            assertThat(parsed, sameInstance(program));
            return List.of(first);
        };
        SemanticRule secondRule = parsed -> {
            assertThat(parsed, sameInstance(program));
            return List.of(second);
        };
        var rules = new ArrayList<>(List.of(firstRule, secondRule));
        var checker = new ProgramChecker(rules);
        rules.clear();
        assertThat(checker.check(program).diagnostics(), contains(first, second));
    }

    @Test
    @DisplayName("Requires non-null rules")
    void rejectsNullRules() {
        assertThrows(NullPointerException.class, () -> new ProgramChecker(null));
        assertThrows(NullPointerException.class, () -> new ProgramChecker(singletonList(null)));
    }
}
