package dev.bounded.compiler.api.check;

import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.diagnostics.DiagnosticCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.bounded.compiler.api.check.ValidCheckMatcher.valid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckResultTest {

    @Test
    @DisplayName("Constructor rejects a null diagnostics list")
    void rejectsNullDiagnosticsList() {
        assertThrows(NullPointerException.class, () -> new CheckResult(null));
    }

    @Test
    @DisplayName("Constructor rejects a diagnostics list containing null")
    void rejectsNullDiagnostic() {
        List<Diagnostic> diagnostics = new ArrayList<>();
        diagnostics.add(null);

        assertThrows(NullPointerException.class, () -> new CheckResult(diagnostics));
    }

    @Test
    @DisplayName("A result without diagnostics is valid")
    void isValidWithoutDiagnostics() {
        var result = CheckResult.validResult();
        assertThat(result, is(valid()));
    }

    @Test
    @DisplayName("A result with diagnostics is invalid")
    void isInvalidWithDiagnostics() {
        var result = new CheckResult(List.of(givenDiagnostic()));
        assertThat(result, is(not(valid())));
    }

    @Test
    @DisplayName("Changes to the original list do not change the result")
    void copiesDiagnostics() {
        var diagnostic = givenDiagnostic();
        var diagnostics = new ArrayList<>(List.of(diagnostic));
        var result = new CheckResult(diagnostics);

        diagnostics.clear();

        assertThat(result.diagnostics(), contains(diagnostic));
        assertThat(result, is(not(valid())));
    }

    @Test
    @DisplayName("The diagnostics list cannot be modified")
    void exposesImmutableDiagnostics() {
        var result = new CheckResult(List.of(givenDiagnostic()));
        var immutableDiagnostics = result.diagnostics();

        assertThrows(UnsupportedOperationException.class, () -> immutableDiagnostics.add(givenDiagnostic()));
        assertThrows(UnsupportedOperationException.class, () -> immutableDiagnostics.set(0, givenDiagnostic()));
        assertThrows(UnsupportedOperationException.class, immutableDiagnostics::removeFirst);
    }

    private Diagnostic givenDiagnostic() {
        return new Diagnostic(
            DiagnosticCode.EFFECT_NOT_ALLOWED,
            new SourceSpan(3, 7),
            "Not allowed"
        );
    }
}
