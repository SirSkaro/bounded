package dev.bounded.compiler.grammar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static dev.bounded.compiler.grammar.GrammarParser.parse;
import static dev.bounded.compiler.grammar.GrammarMatchers.accepted;
import static dev.bounded.compiler.grammar.GrammarMatchers.operation;
import static dev.bounded.compiler.grammar.GrammarMatchers.rejected;
import static org.junit.jupiter.api.Named.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

class BoundedGrammarTest {
    @Nested
    @DisplayName("Full programs")
    class FullPrograms {

        private static Stream<Named<String>> validPrograms() {
            return Stream.of(
                named("an empty value class", "value Money {}"),
                named("an empty entity class", "entity Account {}"),
                named("an empty usecase class", "usecase AccountTransfer {}"),
                named("an empty message class", "message TransferRequest {}"),
                named("a message with a syntactically valid function", "message TransferRequest { function format; }"),
                named("a value with a function", "value Money { function format; }"),
                named("an entity with a query", "entity Account { query balance; }"),
                named("an entity with a command", "entity Account { command deposit; }"),
                named("a value with a syntactically valid command", "value Money { command changeCurrency; }"),
                named("an entity with operation kinds through command grade",
                        "entity Account { function calculate; query balance; command deposit; }"),
                named("an entity with a syntactically valid action", "entity Account { action commit; }"),
                named("a usecase with all supported operation kinds",
                        "usecase AccountTransfer { function format; query hasFunds; command transfer; action commit; }"),
                named("identifiers containing underscores and digits", "entity _Account2 { function deposit; query balance; }"),
                named("tabs and line breaks between tokens", "\tentity Account\n{\r\n function deposit;\tquery balance;\n}\n")
            );
        }
        @DisplayName("Accepts programs allowed by the grammar")
        @ParameterizedTest(name = "Accepts {0}")
        @MethodSource("validPrograms")
        void acceptsValidPrograms(String source) {
            var parseVerdict = parse(source);
            assertThat(parseVerdict, is(accepted()));
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
                named("command used as a class name", "entity command {}"),
                named("command used as an operation name", "entity Account { function command; }"),
                named("a capitalized command keyword", "entity Account { Command deposit; }"),
                named("usecase used as a class name", "entity usecase {}"),
                named("action used as an operation name", "usecase AccountTransfer { function action; }"),
                named("a capitalized usecase keyword", "Usecase AccountTransfer {}"),
                named("a capitalized action keyword", "usecase AccountTransfer { Action commit; }"),
                named("message used as a class name", "value message {}"),
                named("message used as an operation name", "value Money { function message; }"),
                named("a capitalized message keyword", "Message TransferRequest {}"),
                named("an unsupported operation kind", "entity Account { activity deposit; }"),
                named("an unrecognized character", "value Money { @ }")
            );
        }
        @DisplayName("Rejects programs that violate the grammar")
        @ParameterizedTest(name = "Rejects {0}")
        @MethodSource("invalidPrograms")
        void rejectsInvalidPrograms(String source) {
            var parseVerdict = parse(source);
            assertThat(parseVerdict, is(rejected()));
        }

        @Test
        @DisplayName("Recognizes the structure of an entity declaration")
        void recognizesTheStructureOfAnEntityDeclaration() {
            var result = parse("""
                    entity Account {
                        function deposit;
                        query balance;
                    }
                    """);

            assertThat(result, is(accepted()));

            var declaration = result.archeClass();
            assertThat(declaration.kind(), is("entity"));
            assertThat(declaration.name(), is("Account"));
            assertThat(declaration.operations(), contains(
                operation("function", "deposit"),
                operation("query", "balance")
            ));
        }
    }

    @Nested
    @DisplayName("Grammar rule fragments")
    class GrammarRules {
        @DisplayName("Accepts a complete archeclass kind")
        @ParameterizedTest(name = "Accepts {0} as an archeclass kind")
        @ValueSource(strings = {"value", "entity", "usecase", "message"})
        void acceptsArcheClassKinds(String source) {
            assertThat(parse(source, BoundedParser::archeClassKind), is(accepted()));
        }

        @DisplayName("Accepts a complete operation kind")
        @ParameterizedTest(name = "Accepts {0} as an operation kind")
        @ValueSource(strings = {"function", "query", "command", "action"})
        void acceptsOperationKinds(String source) {
            assertThat(parse(source, BoundedParser::archeOperationKind), is(accepted()));
        }

        @DisplayName("Accepts an operation without an enclosing class")
        @ParameterizedTest(name = "Accepts {0} as an operation")
        @ValueSource(strings = {
                "function deposit;",
                "query balance;",
                "command deposit;",
                "action commit;"
        })
        void acceptsOperations(String source) {
            assertThat(parse(source, BoundedParser::archeOperation), is(accepted()));
        }

        private static Stream<Named<String>> invalidOperationKinds() {
            return Stream.of(
                    named("empty input", ""),
                    named("a class keyword", "value"),
                    named("a trailing identifier", "function deposit"),
                    named("a second operation kind", "function query"),
                    named("an invalid character after a valid kind", "function @")
            );
        }
        @DisplayName("Rejects fragments that are not exactly one operation kind")
        @ParameterizedTest(name = "Rejects {0} as an operation kind")
        @MethodSource("invalidOperationKinds")
        void rejectsInvalidOperationKinds(String source) {
            assertThat(parse(source, BoundedParser::archeOperationKind), is(rejected()));
        }

        @Test
        @DisplayName("Rejects an operation missing its semicolon")
        void rejectsAnIncompleteOperation() {
            assertThat(parse("function deposit", BoundedParser::archeOperation), is(rejected()));
        }
    }
}
