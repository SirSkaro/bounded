package dev.bounded.compiler.grammar;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

class BoundedGrammarTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "value Money {}",
            "entity Account {}",
            "value Money { function format; }",
            "entity Account { query balance; }",
            "entity _Account2 { function deposit; query balance; }",
            "\tentity Account\n{\r\n function deposit;\tquery balance;\n}\n"
    })
    void acceptsValidPrograms(String source) {
        assertThat(parse(source), is(accepted()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "Money {}",
            "value {}",
            "value Money {",
            "value Money }",
            "value Money { function format }",
            "value Money { function; }",
            "value Money { format; }",
            "value Money {} entity Account {}",
            "value Money {} trailing",
            "value 2Money {}",
            "value query {}",
            "value Money { @ }"
    })
    void rejectsInvalidPrograms(String source) {
        assertThat(parse(source), is(rejected()));
    }

    @Test
    void recognizesClassAndOperationsInSourceOrder() {
        var result = parse("""
                entity Account {
                    function deposit;
                    query balance;
                }
                """);

        assertThat(result, is(accepted()));
        assertThat(result.classKind(), is("entity"));
        assertThat(result.className(), is("Account"));
        assertThat(result.operations(), contains(
                new Operation("function", "deposit"),
                new Operation("query", "balance")
        ));
    }

    private static GrammarResult parse(String source) {
        var listener = new GrammarErrorListener();
        var lexer = new BoundedLexer(CharStreams.fromString(source));
        var parser = new BoundedParser(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        parser.removeErrorListeners();
        parser.addErrorListener(listener);

        var tree = parser.program();
        return new GrammarResult(source, tree, listener.errors());
    }

    private static GrammarAcceptanceMatcher accepted() {
        return new GrammarAcceptanceMatcher(true);
    }

    private static GrammarAcceptanceMatcher rejected() {
        return new GrammarAcceptanceMatcher(false);
    }

    private static final class GrammarErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String message,
                                RecognitionException exception) {
            errors.add(line + ":" + charPositionInLine + " " + message);
        }

        private List<String> errors() {
            return List.copyOf(errors);
        }
    }

    private record GrammarResult(String source, BoundedParser.ProgramContext tree, List<String> errors) {
        boolean isAccepted() {
            return errors.isEmpty();
        }

        String classKind() {
            return tree.archeClass().archeClassKind().getText();
        }

        String className() {
            return tree.archeClass().IDENTIFIER().getText();
        }

        List<Operation> operations() {
            return tree.archeClass().archeOperation().stream()
                    .map(operation -> new Operation(
                            operation.archeOperationKind().getText(),
                            operation.IDENTIFIER().getText()))
                    .toList();
        }
    }

    private record Operation(String kind, String name) {}

    private static final class GrammarAcceptanceMatcher extends TypeSafeMatcher<GrammarResult> {
        private final boolean expectedAcceptance;

        private GrammarAcceptanceMatcher(boolean expectedAcceptance) {
            this.expectedAcceptance = expectedAcceptance;
        }

        @Override
        protected boolean matchesSafely(GrammarResult result) {
            return result.isAccepted() == expectedAcceptance;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(expectedAcceptance
                    ? "a program accepted by the grammar"
                    : "a program rejected by the grammar");
        }

        @Override
        protected void describeMismatchSafely(GrammarResult result, Description description) {
            description.appendText("source ").appendValue(result.source());
            if (result.isAccepted()) {
                description.appendText(" was accepted without errors");
            } else {
                description.appendText(" was rejected with errors ")
                        .appendValueList("[", ", ", "]", result.errors());
            }
        }
    }
}
