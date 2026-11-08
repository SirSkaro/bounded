package dev.bounded.compiler.acceptance;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

final class CompilationStatusMatcher extends TypeSafeMatcher<CompilationOutcome> {
    private final boolean accepted;

    CompilationStatusMatcher(boolean accepted) {
        this.accepted = accepted;
    }

    @Override
    protected boolean matchesSafely(CompilationOutcome outcome) {
        if (accepted) {
            return outcome.result().succeeded() && outcome.artifactsExist() && !outcome.generatedSourcesPresent();
        }
        return !outcome.result().succeeded() && outcome.result().artifacts().isEmpty() && !outcome.outputCreated();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(accepted ? "accepted with class artifacts and no Java sources"
                : "rejected without artifacts or an output directory");
    }

    @Override
    protected void describeMismatchSafely(CompilationOutcome outcome, Description description) {
        description.appendText(outcome.explanation());
    }
}
