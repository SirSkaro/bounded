package dev.bounded.compiler.grammar;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

final class TokenMatcher extends TypeSafeMatcher<TokenFacade> {
    private final int expectedType;
    private final String expectedText;

    TokenMatcher(int expectedType, String expectedText) {
        this.expectedType = expectedType;
        this.expectedText = expectedText;
    }

    @Override
    protected boolean matchesSafely(TokenFacade token) {
        return token.type() == expectedType && expectedText.equals(token.text());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(BoundedLexer.VOCABULARY.getSymbolicName(expectedType))
                .appendText(" with text ").appendValue(expectedText);
    }

    @Override
    protected void describeMismatchSafely(TokenFacade token, Description description) {
        description.appendText("was ").appendValue(token);
    }
}
