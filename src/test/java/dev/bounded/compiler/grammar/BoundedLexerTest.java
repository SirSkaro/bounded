package dev.bounded.compiler.grammar;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static dev.bounded.compiler.grammar.BoundedLexer.ACTION;
import static dev.bounded.compiler.grammar.BoundedLexer.COMMAND;
import static dev.bounded.compiler.grammar.BoundedLexer.MESSAGE;
import static dev.bounded.compiler.grammar.BoundedLexer.USECASE;
import static dev.bounded.compiler.grammar.BoundedLexer.VALUE;
import static dev.bounded.compiler.grammar.BoundedLexer.ENTITY;
import static dev.bounded.compiler.grammar.BoundedLexer.FUNCTION;
import static dev.bounded.compiler.grammar.BoundedLexer.QUERY;
import static dev.bounded.compiler.grammar.BoundedLexer.IDENTIFIER;
import static dev.bounded.compiler.grammar.BoundedLexer.LBRACE;
import static dev.bounded.compiler.grammar.BoundedLexer.RBRACE;
import static dev.bounded.compiler.grammar.BoundedLexer.SEMICOLON;
import static dev.bounded.compiler.grammar.LexerMatchers.hasTokens;
import static dev.bounded.compiler.grammar.LexerMatchers.token;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Named.named;

class BoundedLexerTest {
    @Test
    @DisplayName("Tokenizes the 'function' archeop")
    void tokenizesFunctionFragment() {
        assertThat(lex("function deposit"), hasTokens(
            token(FUNCTION, "function"),
            token(IDENTIFIER, "deposit")
        ));
    }

    @Test
    @DisplayName("Tokenizes the 'query' archeop")
    void tokenizesQueryFragment() {
        assertThat(lex("query balance"), hasTokens(
                token(QUERY, "query"),
                token(IDENTIFIER, "balance")
        ));
    }

    @Test
    @DisplayName("Tokenizes the 'command' archeop without reserving identifiers with its prefix")
    void tokenizesCommandFragment() {
        assertThat(lex("command commander;"), hasTokens(
                token(COMMAND, "command"),
                token(IDENTIFIER, "commander"),
                token(SEMICOLON, ";")
        ));
    }

    @Test
    @DisplayName("Tokenizes the 'action' archeop without reserving identifiers with its prefix")
    void tokenizesActionFragment() {
        assertThat(lex("action actionPlan;"), hasTokens(
                token(ACTION, "action"),
                token(IDENTIFIER, "actionPlan"),
                token(SEMICOLON, ";")
        ));
    }

    @Test
    @DisplayName("Tokenizes the 'value' archeclass")
    void tokenizesValueFragment() {
        assertThat(lex("value Money"), hasTokens(
                token(VALUE, "value"),
                token(IDENTIFIER, "Money")
        ));
    }

    @Test
    @DisplayName("Tokenizes the 'entity' archeclass")
    void tokenizesEntityFragment() {
        assertThat(lex("entity Account"), hasTokens(
                token(ENTITY, "entity"),
                token(IDENTIFIER, "Account")
        ));
    }

    @Test
    @DisplayName("Tokenizes the 'usecase' archeclass without reserving identifiers with its prefix")
    void tokenizesUseCaseFragment() {
        assertThat(lex("usecase usecaseRunner"), hasTokens(
                token(USECASE, "usecase"),
                token(IDENTIFIER, "usecaseRunner")
        ));
    }

    @Test
    @DisplayName("Tokenizes the 'message' archeclass without reserving identifiers with its prefix")
    void tokenizesMessageFragment() {
        assertThat(lex("message messageEnvelope"), hasTokens(
                token(MESSAGE, "message"),
                token(IDENTIFIER, "messageEnvelope")
        ));
    }

    @Test
    @DisplayName("Skips whitespace between tokens")
    void skipsWhitespaceBetweenTokens() {
        assertThat(lex("\tfunction\r\n deposit \t;\n"), hasTokens(
                token(FUNCTION, "function"),
                token(IDENTIFIER, "deposit"),
                token(SEMICOLON, ";")
        ));
    }

    @Test
    @DisplayName("Distinguishes a keyword from an identifier with the same prefix")
    void distinguishesKeywordsFromIdentifiers() {
        assertThat(lex("function functionality"), hasTokens(
                token(FUNCTION, "function"),
                token(IDENTIFIER, "functionality")
        ));
    }

    @Test
    @DisplayName("Recognizes punctuation adjacent to an identifier")
    void recognizesAdjacentPunctuation() {
        assertThat(lex("{_Account2;}"), hasTokens(
                token(LBRACE, "{"),
                token(IDENTIFIER, "_Account2"),
                token(SEMICOLON, ";"),
                token(RBRACE, "}")
        ));
    }

    @DisplayName("Produces no tokens for empty or whitespace-only input")
    @ParameterizedTest(name = "Produces no tokens for {0}")
    @MethodSource("blankFragments")
    void producesNoTokensForBlankInput(String source) {
        assertThat(lex(source), hasTokens());
    }

    private static Stream<Named<String>> blankFragments() {
        return Stream.of(
            named("empty input", ""),
            named("whitespace-only input", " \t\r\n")
        );
    }

    @Test
    @DisplayName("Reports an unrecognized character in a fragment")
    void reportsAnUnrecognizedCharacter() {
        assertThat(lex("function @deposit;").errors(), not(empty()));
    }

    private static LexerResult lex(String source) {
        var listener = new GrammarErrorListener();
        var lexer = new BoundedLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);

        var tokens = lexer.getAllTokens().stream().map(TokenFacade::new).toList();
        return new LexerResult(source, tokens, listener.errors());
    }
}
