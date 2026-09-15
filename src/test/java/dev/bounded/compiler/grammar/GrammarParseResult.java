package dev.bounded.compiler.grammar;

import java.util.List;
import java.util.Optional;

interface GrammarParseResult {
    String source();

    List<String> errors();

    default Optional<TokenFacade> unconsumedToken() {
        return Optional.empty();
    }

    default boolean isAccepted() {
        return errors().isEmpty() && unconsumedToken().isEmpty();
    }
}
