package dev.bounded.compiler.grammar;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;

final class LexerTokensMatcher extends TypeSafeMatcher<LexerResult> {
    private final Matcher<Iterable<? extends TokenFacade>> expectedTokens;

    LexerTokensMatcher(List<Matcher<? super TokenFacade>> tokens) {
        expectedTokens = tokens.isEmpty() ? emptyIterable() : contains(tokens);
    }

    @Override
    protected boolean matchesSafely(LexerResult result) {
        return !result.hasErrors() && expectedTokens.matches(result.tokens());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("tokens ").appendDescriptionOf(expectedTokens)
                .appendText(" without lexer errors");
    }

    @Override
    protected void describeMismatchSafely(LexerResult result, Description description) {
        description.appendText("source ").appendValue(result.source());
        if (result.hasErrors()) {
            description.appendText(" had lexer errors ")
                    .appendValueList("[", ", ", "]", result.errors());
        } else {
            description.appendText(" had tokens ");
            expectedTokens.describeMismatch(result.tokens(), description);
        }
    }
}
