package dev.bounded.compiler.acceptance;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

final class DiagnosticCodeMatcher extends TypeSafeMatcher<CompilationOutcome> {
    private final String code;

    DiagnosticCodeMatcher(String code) {
        this.code = code;
    }

    @Override
    protected boolean matchesSafely(CompilationOutcome outcome) {
        return outcome.result().diagnostics().stream()
                .anyMatch(diagnostic -> code.equals(diagnostic.code().externalCode()));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a diagnostic with code ").appendValue(code);
    }

    @Override
    protected void describeMismatchSafely(CompilationOutcome outcome, Description description) {
        description.appendText(outcome.explanation());
    }
}
