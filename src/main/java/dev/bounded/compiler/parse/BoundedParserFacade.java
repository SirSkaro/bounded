package dev.bounded.compiler.parse;

import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.api.parse.ParseResult;
import dev.bounded.compiler.api.parse.Parser;
import dev.bounded.compiler.grammar.BoundedLexer;
import dev.bounded.compiler.grammar.BoundedParser;
import org.antlr.v4.runtime.*;

import static dev.bounded.compiler.api.parse.ParseResult.invalidResult;

public final class BoundedParserFacade implements Parser {
    private final BoundedAstMapper mapper = new BoundedAstMapper();

    @Override
    public ParseResult parse(SourceFile sourceFile) {
        var input = CharStreams.fromString(sourceFile.content(), sourceFile.name());
        var errors = new SyntaxErrorCollector(sourceFile.name());
        var parsedInput = parseWithListeners(input, errors);

        return resultOf(parsedInput, errors, sourceFile.name());
    }

    private BoundedParser parseWithListeners(CodePointCharStream input, ANTLRErrorListener... listeners) {
        var lexer = new BoundedLexer(input);
        var parser = new BoundedParser(new CommonTokenStream(lexer));

        setListeners(lexer, listeners);
        setListeners(parser, listeners);

        return parser;
    }

    private ParseResult resultOf(BoundedParser parsedInput, SyntaxErrorCollector errors, String sourceName) {
        var parseTree = parsedInput.program();
        var diagnostics = errors.diagnostics();

        if (!diagnostics.isEmpty()) {
            return invalidResult(diagnostics);
        }

        var ast = mapper.map(parseTree, sourceName);
        return ParseResult.validResult(ast);
    }

    private void setListeners(Recognizer<?, ?> recognizer, ANTLRErrorListener... listeners) {
        recognizer.removeErrorListeners();
        for(var listener: listeners) {
            recognizer.addErrorListener(listener);
        }
    }

}
