package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.diagnostics.DiagnosticCode;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

final class SyntaxErrorCollector extends BaseErrorListener {
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String message,
            RecognitionException exception
    ) {
        diagnostics.add(new Diagnostic(
                DiagnosticCode.SYNTAX_ERROR,
                new SourceSpan(line, charPositionInLine + 1),
                message));
    }

    List<Diagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }
}
