package dev.bounded.compiler.parse;

import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.grammar.BoundedLexer;
import dev.bounded.compiler.grammar.BoundedParser;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Optional;

public final class BoundedParserFacade {
    private final BoundedAstMapper mapper = new BoundedAstMapper();

    public ParseResult parse(SourceFile sourceFile) {
        var input = CharStreams.fromString(sourceFile.content(), sourceFile.name());
        var errors = new SyntaxErrorCollector();

        var lexer = lexerWithListeners(input, errors);
        var parser = parserWithListeners(lexer, errors);

        var parseTree = parser.program();
        var diagnostics = errors.diagnostics();
        if (!diagnostics.isEmpty()) {
            return new ParseResult(Optional.empty(), diagnostics);
        }

        return new ParseResult(Optional.of(mapper.map(parseTree)), diagnostics);
    }

    private BoundedLexer lexerWithListeners(CodePointCharStream input, ANTLRErrorListener... listeners) {
        var lexer = new BoundedLexer(input);

        lexer.removeErrorListeners();
        for(var listener: listeners) {
            lexer.addErrorListener(listener);
        }

        return lexer;
    }

    private BoundedParser parserWithListeners(BoundedLexer lexer, ANTLRErrorListener... listeners) {
        var parser = new BoundedParser(new CommonTokenStream(lexer));

        parser.removeErrorListeners();
        for(var listener: listeners) {
            lexer.addErrorListener(listener);
        }

        return parser;
    }

}
