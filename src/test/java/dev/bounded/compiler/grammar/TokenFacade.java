package dev.bounded.compiler.grammar;

import org.antlr.v4.runtime.Token;

final class TokenFacade {
    private final Token token;

    TokenFacade(Token token) {
        this.token = token;
    }

    int type() {
        return token.getType();
    }

    String text() {
        return token.getText();
    }

    @Override
    public String toString() {
        return BoundedLexer.VOCABULARY.getSymbolicName(type()) + "(\"" + text() + "\")";
    }
}
