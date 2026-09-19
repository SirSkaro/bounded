package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.bounded.compiler.ast.ArcheClassKind.VALUE;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.SYNTAX_ERROR;
import static dev.bounded.compiler.parse.ParseResult.invalidResult;
import static dev.bounded.compiler.parse.ParseResult.validResult;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParseResultTest {
    @Test
    @DisplayName("A valid result contains the supplied program")
    void retainsTheProgram() {
        var expectedProgram = givenProgram();

        var result = validResult(expectedProgram);
        var actualProgram = result.program().orElseThrow();

        assertThat(actualProgram, is(sameInstance(expectedProgram)));
    }

    @Test
    @DisplayName("A valid result has no diagnostics")
    void hasNoDiagnosticsForAValidProgram() {
        var result = validResult(givenProgram());

        assertThat(result.diagnostics(), is(empty()));
    }

    @Test
    @DisplayName("A valid result requires a non-null program")
    void rejectsANullProgram() {
        assertThrows(NullPointerException.class, () -> validResult(null));
    }

    @Test
    @DisplayName("An invalid result has no program")
    void hasNoProgramForAnInvalidResult() {
        var result = invalidResult(List.of(givenDiagnostic()));

        assertThat(result.program(), is(Optional.empty()));
    }

    @Test
    @DisplayName("An invalid result preserves diagnostics in their supplied order")
    void preservesDiagnosticOrder() {
        var first = givenDiagnostic();
        var second = new Diagnostic(SYNTAX_ERROR, new SourceSpan(4, 1), "Missing closing brace.");

        var result = invalidResult(List.of(first, second));

        assertThat(result.diagnostics(), contains(first, second));
    }

    @Test
    @DisplayName("Changes to the original diagnostics list do not change the result")
    void copiesDiagnostics() {
        var diagnostic = givenDiagnostic();
        var diagnostics = new ArrayList<>(List.of(diagnostic));
        var result = new ParseResult(Optional.empty(), diagnostics);

        diagnostics.clear();

        assertThat(result.diagnostics(), contains(diagnostic));
    }

    @Test
    @DisplayName("The returned diagnostics cannot be modified")
    void exposesImmutableDiagnostics() {
        var result = invalidResult(List.of(givenDiagnostic()));
        var diagnostics = result.diagnostics();

        assertThrows(UnsupportedOperationException.class, () -> diagnostics.add(givenDiagnostic()));
        assertThrows(UnsupportedOperationException.class, () -> diagnostics.set(0, givenDiagnostic()));
        assertThrows(UnsupportedOperationException.class, diagnostics::removeFirst);
    }

    @Test
    @DisplayName("Rejects a null diagnostics list")
    void rejectsNullDiagnostics() {
        assertThrows(NullPointerException.class, () -> new ParseResult(Optional.empty(), null));
    }

    @Test
    @DisplayName("Rejects a diagnostics list containing null")
    void rejectsANullDiagnostic() {
        var diagnostics = new ArrayList<Diagnostic>();
        diagnostics.add(null);

        assertThrows(NullPointerException.class, () -> new ParseResult(Optional.empty(), diagnostics));
    }

    @Test
    @DisplayName("Rejects null optional of program")
    void rejectsANullOptional() {
        assertThrows(NullPointerException.class, () -> new ParseResult(null, List.of()));
    }

    @Test
    @DisplayName("Rejects invariant of empty program with no diagnostics")
    void rejectsBothEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new ParseResult(Optional.empty(), List.of()));
    }

    @Test
    @DisplayName("Rejects invariant of valid program with error diagnostics")
    void rejectsBothPopulated() {
        assertThrows(IllegalArgumentException.class, () -> new ParseResult(Optional.of(givenProgram()), List.of(givenDiagnostic())));
    }

    private BoundedProgram givenProgram() {
        return new BoundedProgram(new ArcheClassDeclaration(
            VALUE, "Money", List.of(), new SourceSpan(1, 1)
        ));
    }

    private Diagnostic givenDiagnostic() {
        return new Diagnostic(SYNTAX_ERROR, new SourceSpan(2, 3), "Missing semicolon.");
    }
}
