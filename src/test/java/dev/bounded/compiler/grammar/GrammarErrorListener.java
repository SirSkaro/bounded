package dev.bounded.compiler.grammar;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

final class GrammarErrorListener extends BaseErrorListener {
    private final List<String> errors = new ArrayList<>();

    @Override
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String message,
                            RecognitionException exception) {
        errors.add(line + ":" + charPositionInLine + " " + message);
    }

    List<String> errors() {
        return List.copyOf(errors);
    }
}
