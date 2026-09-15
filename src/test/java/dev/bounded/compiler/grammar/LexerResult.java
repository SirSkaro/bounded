package dev.bounded.compiler.grammar;

import java.util.List;

import static java.util.List.copyOf;

record LexerResult(String source, List<TokenFacade> tokens, List<String> errors) {
    LexerResult {
        tokens = copyOf(tokens);
        errors = copyOf(errors);
    }

    boolean hasErrors() {
        return !errors.isEmpty();
    }
}
