package dev.bounded.compiler.grammar;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.Optional;
import java.util.function.Function;

final class GrammarParser {
    private GrammarParser() {}

    static GrammarResult parse(String source) {
        var result = parse(source, BoundedParser::program);
        return new GrammarResult(source, result.tree(), result.errors());
    }

    static <T extends ParserRuleContext> RuleResult<T> parse(String source, Function<BoundedParser, T> rule) {
        var listener = new GrammarErrorListener();
        var lexer = new BoundedLexer(CharStreams.fromString(source));
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);

        var tokens = new CommonTokenStream(lexer);
        tokens.fill();
        var parser = new BoundedParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        var tree = rule.apply(parser);
        var nextToken = parser.getCurrentToken();
        var unconsumedToken = nextToken.getType() == Token.EOF
                ? Optional.<TokenFacade>empty()
                : Optional.of(new TokenFacade(nextToken));
        return new RuleResult<>(source, tree, listener.errors(), unconsumedToken);
    }
}
