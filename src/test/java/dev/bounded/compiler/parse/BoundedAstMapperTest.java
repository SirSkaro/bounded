package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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
import static org.junit.jupiter.params.provider.Arguments.arguments;

class BoundedAstMapperTest {
    private final BoundedAstMapper mapper = new BoundedAstMapper();

    private static Stream<Arguments> classKinds() {
        return Stream.of(
                arguments(VALUE, ArcheClassKind.VALUE),
                arguments(ENTITY, ArcheClassKind.ENTITY)
        );
    }
    @DisplayName("Maps class kinds from the supplied context")
    @ParameterizedTest(name = "Maps the class kind to {1}")
    @MethodSource("classKinds")
    void mapsClassKinds(int tokenType, ArcheClassKind expectedKind) {
        var context = program(archeClass(tokenType, "Account"));

        var kind = mapper.map(context).archeClass().kind();

        assertThat(kind, is(expectedKind));
    }

    @Test
    @DisplayName("Preserves the class identifier from the supplied context")
    void preservesTheClassName() {
        var expectedName = "MyName";
        var context = program(archeClass(ENTITY, expectedName));

        var name = mapper.map(context).archeClass().name();

        assertThat(name, is(expectedName));
    }

    private static Stream<Arguments> operationKinds() {
        return Stream.of(
                arguments(FUNCTION, ArcheOperationKind.FUNCTION),
                arguments(QUERY, ArcheOperationKind.QUERY)
        );
    }
    @DisplayName("Maps operation kinds from the supplied context")
    @ParameterizedTest(name = "Maps the operation kind to {1}")
    @MethodSource("operationKinds")
    void mapsOperationKinds(int tokenType, ArcheOperationKind expectedKind) {
        var context = program(archeClass(ENTITY, "Account", operation(tokenType, "balance")));

        var operation = mapper.map(context)
                .archeClass()
                .operations()
                .getFirst();

        assertThat(operation.kind(), is(expectedKind));
    }

    @Test
    @DisplayName("Preserves the operation identifier from the supplied context")
    void preservesTheOperationName() {
        var expectedName = "opName";
        var context = program(archeClass(ENTITY, "Account", operation(QUERY, expectedName)));

        var operation = mapper.map(context)
                .archeClass()
                .operations()
                .getFirst();

        assertThat(operation.name(), is(expectedName));
    }

    @Test
    @DisplayName("Maps a class without operation contexts to an empty operations list")
    void mapsAnEmptyClass() {
        var context = program(archeClass(VALUE, "Money"));

        var operations = mapper.map(context)
                .archeClass()
                .operations();

        assertThat(operations, is(empty()));
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

        var names = mapper.map(context)
                .archeClass()
                .operations()
                .stream()
                .map(ArcheOperationDeclaration::name)
                .toList();

        assertThat(names, contains("deposit", "balance", "withdraw"));
    }

    @DisplayName("Maps the class start token to a one-based source span")
    @ParameterizedTest(name = "Line {0}, character position {1} becomes column {2}")
    @CsvSource({
        "1, 0, 1",
        "7, 4, 5"
    })
    void mapsTheClassSourceSpan(int line, int characterPosition, int expectedColumn) {
        var context = program(at(archeClass(ENTITY, "Account"), line, characterPosition));

        var span = mapper.map(context)
                .archeClass()
                .span();

        assertThat(span, is(new SourceSpan(line, expectedColumn)));
    }

    @Test
    @DisplayName("Maps each operation's own start token to a one-based source span")
    void mapsOperationSourceSpans() {
        var context = program(archeClass(ENTITY, "Account",
                at(operation(FUNCTION, "deposit"), 3, 0),
                at(operation(QUERY, "balance"), 8, 4)
        ));

        var spans = mapper.map(context)
                .archeClass()
                .operations()
                .stream()
                .map(ArcheOperationDeclaration::span)
                .toList();

        assertThat(spans, contains(
            new SourceSpan(3, 1),
            new SourceSpan(8, 5))
        );
    }
}
