package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static dev.bounded.compiler.grammar.BoundedParser.COMMAND;
import static dev.bounded.compiler.grammar.BoundedParser.ENTITY;
import static dev.bounded.compiler.grammar.BoundedParser.FUNCTION;
import static dev.bounded.compiler.grammar.BoundedParser.QUERY;
import static dev.bounded.compiler.grammar.BoundedParser.VALUE;
import static dev.bounded.compiler.parse.MapperContextFixtures.archeClass;
import static dev.bounded.compiler.parse.MapperContextFixtures.at;
import static dev.bounded.compiler.parse.MapperContextFixtures.operation;
import static dev.bounded.compiler.parse.MapperContextFixtures.program;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


class BoundedAstMapperTest {
    private final BoundedAstMapper mapper = new BoundedAstMapper();

    @Nested
    @DisplayName("Archeclasses")
    class ArcheClasses {
        @Test
        @DisplayName("Maps a value context to the AST class kind")
        void mapsValueClassKind() {
            var context = program(archeClass(VALUE, "Account"));

            var kind = mapper.map(context, "account.bounded").archeClass().kind();

            assertThat(kind, is(ArcheClassKind.VALUE));
        }

        @Test
        @DisplayName("Maps an entity context to the AST class kind")
        void mapsEntityClassKind() {
            var context = program(archeClass(ENTITY, "Account"));

            var kind = mapper.map(context, "account.bounded").archeClass().kind();

            assertThat(kind, is(ArcheClassKind.ENTITY));
        }

        @Test
        @DisplayName("Preserves the class identifier from the supplied context")
        void preservesTheClassName() {
            var expectedName = "MyName";
            var context = program(archeClass(ENTITY, expectedName));

            var name = mapper.map(context, "account.bounded").archeClass().name();

            assertThat(name, is(expectedName));
        }

        @Test
        @DisplayName("Maps a class without operation contexts to an empty operations list")
        void mapsAnEmptyClass() {
            var context = program(archeClass(VALUE, "Money"));

            var operations = mapper.map(context, "account.bounded")
                    .archeClass()
                    .operations();

            assertThat(operations, is(empty()));
        }
    }

    @Nested
    @DisplayName("Archeoperations")
    class ArcheOperations {
        @Test
        @DisplayName("Maps a function context to the AST operation kind")
        void mapsFunctionOperationKind() {
            var context = program(archeClass(ENTITY, "Account", operation(FUNCTION, "balance")));

            var operation = mapper.map(context, "account.bounded")
                    .archeClass()
                    .operations()
                    .getFirst();

            assertThat(operation.kind(), is(ArcheOperationKind.FUNCTION));
        }

        @Test
        @DisplayName("Maps a query context to the AST operation kind")
        void mapsQueryOperationKind() {
            var context = program(archeClass(ENTITY, "Account", operation(QUERY, "balance")));

            var operation = mapper.map(context, "account.bounded")
                    .archeClass()
                    .operations()
                    .getFirst();

            assertThat(operation.kind(), is(ArcheOperationKind.QUERY));
        }

        @Test
        @DisplayName("Maps a command context with its name and source span")
        void mapsCommandOperation() {
            var context = program(archeClass(ENTITY, "Account",
                    at(operation(COMMAND, "deposit"), 4, 6)));

            var operation = mapper.map(context, "account.bounded").archeClass().operations().getFirst();

            assertThat(operation, is(new ArcheOperationDeclaration(
                    ArcheOperationKind.COMMAND, "deposit", new SourceSpan("account.bounded", 4, 7))));
        }

        @Test
        @DisplayName("Preserves the operation identifier from the supplied context")
        void preservesTheOperationName() {
            var expectedName = "opName";
            var context = program(archeClass(ENTITY, "Account", operation(QUERY, expectedName)));

            var operation = mapper.map(context, "account.bounded")
                    .archeClass()
                    .operations()
                    .getFirst();

            assertThat(operation.name(), is(expectedName));
        }

        @Test
        @DisplayName("Preserves the order of the supplied operation contexts")
        void preservesOperationOrder() {
            var context = program(
                archeClass(ENTITY, "Account",
                operation(FUNCTION, "deposit"),
                operation(QUERY, "balance"),
                operation(FUNCTION, "withdraw")
            ));

            var names = mapper.map(context, "account.bounded")
                    .archeClass()
                    .operations()
                    .stream()
                    .map(ArcheOperationDeclaration::name)
                    .toList();

            assertThat(names, contains("deposit", "balance", "withdraw"));
        }
    }

    @Nested
    @DisplayName("Source spans")
    class SourceSpans {
        @DisplayName("Maps the class start token to a one-based source span")
        @ParameterizedTest(name = "Line {0}, character position {1} becomes column {2}")
        @CsvSource({
            "1, 0, 1",
            "7, 4, 5"
        })
        void mapsTheClassSourceSpan(int line, int characterPosition, int expectedColumn) {
            var context = program(at(archeClass(ENTITY, "Account"), line, characterPosition));

            var span = mapper.map(context, "account.bounded")
                    .archeClass()
                    .span();

            assertThat(span, is(new SourceSpan("account.bounded", line, expectedColumn)));
        }

        @Test
        @DisplayName("Maps each operation's own start token to a one-based source span")
        void mapsOperationSourceSpans() {
            var context = program(archeClass(ENTITY, "Account",
                    at(operation(FUNCTION, "deposit"), 3, 0),
                    at(operation(QUERY, "balance"), 8, 4)
            ));

            var spans = mapper.map(context, "account.bounded")
                    .archeClass()
                    .operations()
                    .stream()
                    .map(ArcheOperationDeclaration::span)
                    .toList();

            assertThat(spans, contains(
                new SourceSpan("account.bounded", 3, 1),
                new SourceSpan("account.bounded", 8, 5))
            );
        }
    }
}
