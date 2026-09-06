package dev.bounded.compiler.parse;

import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.grammar.BoundedLexer;
import dev.bounded.compiler.grammar.BoundedParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Optional;

public final class BoundedParserFacade {
    private final BoundedAstMapper mapper = new BoundedAstMapper();

    public ParseResult parse(SourceFile sourceFile) {
        var input = CharStreams.fromString(sourceFile.content(), sourceFile.name());
        var lexer = new BoundedLexer(input);
        var parser = new BoundedParser(new CommonTokenStream(lexer));
        var errors = new SyntaxErrorCollector();

        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errors);
        parser.addErrorListener(errors);

        var parseTree = parser.program();
        var diagnostics = errors.diagnostics();
        if (!diagnostics.isEmpty()) {
            return new ParseResult(Optional.empty(), diagnostics);
        }

        return new ParseResult(Optional.of(mapper.map(parseTree)), diagnostics);
    }
}
