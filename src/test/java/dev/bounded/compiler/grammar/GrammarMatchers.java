package dev.bounded.compiler.grammar;

import org.hamcrest.Matcher;

final class GrammarMatchers {
    private GrammarMatchers() {}

    static Matcher<GrammarResult> accepted() {
        return new GrammarAcceptanceMatcher(true);
    }

    static Matcher<GrammarResult> rejected() {
        return new GrammarAcceptanceMatcher(false);
    }

    static Matcher<OperationFacade> operation(String kind, String name) {
        return new OperationMatcher(kind, name);
    }
}
