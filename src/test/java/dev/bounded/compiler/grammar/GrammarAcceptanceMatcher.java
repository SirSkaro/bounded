package dev.bounded.compiler.grammar;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

final class GrammarAcceptanceMatcher extends TypeSafeMatcher<GrammarResult> {
    private final boolean expectedAcceptance;

    GrammarAcceptanceMatcher(boolean expectedAcceptance) {
        this.expectedAcceptance = expectedAcceptance;
    }

    @Override
    protected boolean matchesSafely(GrammarResult result) {
        return result.isAccepted() == expectedAcceptance;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expectedAcceptance
                ? "a program accepted by the grammar"
                : "a program rejected by the grammar");
    }

    @Override
    protected void describeMismatchSafely(GrammarResult result, Description description) {
        description.appendText("source ").appendValue(result.source());
        if (result.isAccepted()) {
            description.appendText(" was accepted without errors");
        } else {
            description.appendText(" was rejected with errors ")
                    .appendValueList("[", ", ", "]", result.errors());
        }
    }
}
