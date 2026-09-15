package dev.bounded.compiler.grammar;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

final class GrammarAcceptanceMatcher extends TypeSafeMatcher<GrammarParseResult> {
    private final boolean expectedAcceptance;

    GrammarAcceptanceMatcher(boolean expectedAcceptance) {
        this.expectedAcceptance = expectedAcceptance;
    }

    @Override
    protected boolean matchesSafely(GrammarParseResult result) {
        return result.isAccepted() == expectedAcceptance;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expectedAcceptance
                ? "source fully accepted by the selected grammar rule"
                : "source rejected by the selected grammar rule");
    }

    @Override
    protected void describeMismatchSafely(GrammarParseResult result, Description description) {
        description.appendText("source ").appendValue(result.source());
        if (result.isAccepted()) {
            description.appendText(" was fully consumed without errors");
        } else {
            if (!result.errors().isEmpty()) {
                description.appendText(" had errors ")
                        .appendValueList("[", ", ", "]", result.errors());
            }
            result.unconsumedToken().ifPresent(token ->
                    description.appendText(" left unconsumed token ").appendValue(token));
        }
    }
}
