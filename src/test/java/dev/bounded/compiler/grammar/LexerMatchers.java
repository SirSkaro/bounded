package dev.bounded.compiler.grammar;

import org.hamcrest.Matcher;

import java.util.List;

final class LexerMatchers {
    private LexerMatchers() {}

    static Matcher<TokenFacade> token(int type, String text) {
        return new TokenMatcher(type, text);
    }

    @SafeVarargs
    static Matcher<LexerResult> hasTokens(Matcher<? super TokenFacade>... tokens) {
        return new LexerTokensMatcher(List.of(tokens));
    }
}
