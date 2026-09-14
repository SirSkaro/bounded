package dev.bounded.compiler.grammar;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static dev.bounded.compiler.grammar.GrammarMatchers.accepted;
import static dev.bounded.compiler.grammar.GrammarMatchers.rejected;
import static org.junit.jupiter.api.Named.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

class BoundedGrammarTest {

    private static Stream<Named<String>> validPrograms() {
        return Stream.of(
            named("an empty value class", "value Money {}"),
            named("an empty entity class", "entity Account {}"),
            named("a value with a function", "value Money { function format; }"),
            named("an entity with a query", "entity Account { query balance; }"),
            named("identifiers containing underscores and digits", "entity _Account2 { function deposit; query balance; }"),
            named("tabs and line breaks between tokens", "\tentity Account\n{\r\n function deposit;\tquery balance;\n}\n")
        );
    }
    @DisplayName("Accepts programs allowed by the grammar")
    @ParameterizedTest(name = "Accepts {0}")
    @MethodSource("validPrograms")
    void acceptsValidPrograms(String source) {
        assertThat(parse(source), is(accepted()));
    }

    private static Stream<Named<String>> invalidPrograms() {
        return Stream.of(
            named("empty input", ""),
            named("a class without a kind", "Money {}"),
            named("a class without a name", "value {}"),
            named("a missing closing brace", "value Money {"),
            named("a missing opening brace", "value Money }"),
            named("an operation without a semicolon", "value Money { function format }"),
            named("an operation without a name", "value Money { function; }"),
            named("an operation without a kind", "value Money { format; }"),
            named("multiple class declarations", "value Money {} entity Account {}"),
            named("trailing input after a class", "value Money {} trailing"),
            named("an identifier starting with a digit", "value 2Money {}"),
            named("a keyword used as a class name", "value query {}"),
            named("an unrecognized character", "value Money { @ }")
        );
    }
    @DisplayName("Rejects programs that violate the grammar")
    @ParameterizedTest(name = "Rejects {0}")
    @MethodSource("invalidPrograms")
    void rejectsInvalidPrograms(String source) {
        assertThat(parse(source), is(rejected()));
    }

    @Test
    @DisplayName("Recognizes the class kind and name and preserves operation order")
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
                new OperationFacade("function", "deposit"),
                new OperationFacade("query", "balance")
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
}
