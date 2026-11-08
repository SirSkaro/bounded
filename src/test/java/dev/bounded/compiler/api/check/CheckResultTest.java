package dev.bounded.compiler.api.check;

import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.ast.AnalyzedProgram;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.diagnostics.DiagnosticCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.bounded.compiler.api.check.ValidCheckMatcher.valid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckResultTest {
    private final AnalyzedProgram program = new AnalyzedProgram(new BoundedProgram(
            new ArcheClassDeclaration(ArcheClassKind.VALUE, "Money", List.of(), new SourceSpan("money.bounded", 1, 1))));

    @Test
    @DisplayName("Invalid result rejects a null diagnostics list")
    void rejectsNullDiagnosticsList() {
        assertThrows(NullPointerException.class, () -> CheckResult.invalidResult(null));
    }

    @Test
    @DisplayName("Invalid result rejects a diagnostics list containing null")
    void rejectsNullDiagnostic() {
        List<Diagnostic> diagnostics = new ArrayList<>();
        diagnostics.add(null);

        assertThrows(NullPointerException.class, () -> CheckResult.invalidResult(diagnostics));
    }

    @Test
    @DisplayName("A result without diagnostics is valid")
    void isValidWithoutDiagnostics() {
        var result = CheckResult.validResult(program);
        assertThat(result, is(valid()));
        assertThat(result.program(), sameInstance(program));
        assertThat(result.diagnostics(), is(empty()));
    }

    @Test
    @DisplayName("A result with diagnostics is invalid")
    void isInvalidWithDiagnostics() {
        var result = CheckResult.invalidResult(List.of(givenDiagnostic()));
        assertThat(result, is(not(valid())));
        assertThat(result.program(), is(nullValue()));
    }

    @Test
    @DisplayName("Changes to the original list do not change the result")
    void copiesDiagnostics() {
        var diagnostic = givenDiagnostic();
        var diagnostics = new ArrayList<>(List.of(diagnostic));
        var result = CheckResult.invalidResult(diagnostics);

        diagnostics.clear();

        assertThat(result.diagnostics(), contains(diagnostic));
        assertThat(result, is(not(valid())));
    }

    @Test
    @DisplayName("The diagnostics list cannot be modified")
    void exposesImmutableDiagnostics() {
        var result = CheckResult.invalidResult(List.of(givenDiagnostic()));
        var immutableDiagnostics = result.diagnostics();

        assertThrows(UnsupportedOperationException.class, () -> immutableDiagnostics.add(givenDiagnostic()));
        assertThrows(UnsupportedOperationException.class, () -> immutableDiagnostics.set(0, givenDiagnostic()));
        assertThrows(UnsupportedOperationException.class, immutableDiagnostics::removeFirst);
    }

    @Test
    @DisplayName("A valid result requires an analyzed program")
    void rejectsNullProgram() {
        assertThrows(NullPointerException.class, () -> CheckResult.validResult(null));
    }

    @Test
    @DisplayName("An invalid result requires at least one diagnostic")
    void rejectsEmptyDiagnostics() {
        assertThrows(IllegalArgumentException.class, () -> CheckResult.invalidResult(List.of()));
    }

    private Diagnostic givenDiagnostic() {
        return new Diagnostic(
            DiagnosticCode.EFFECT_NOT_ALLOWED,
            new SourceSpan("money.bounded", 3, 7),
            "Not allowed"
        );
    }
}
