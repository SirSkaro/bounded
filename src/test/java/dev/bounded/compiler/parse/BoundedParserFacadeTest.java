package dev.bounded.compiler.parse;

import org.junit.jupiter.api.Test;

import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class BoundedParserFacadeTest {
    private final BoundedParserFacade parser = new BoundedParserFacade();

    @Test
    @DisplayName("Maps source to the AST with source spans")
    void mapsSourceTextToTheBoundedAst() {
        var source = new SourceFile("money.bounded", """
                value Money {
                  function format;
                }
                """);

        var result = parser.parse(source);

        assertThat(result.isValid(), is(true));
        var archeClass = result.program().archeClass();
        assertThat(archeClass.kind(), is(ArcheClassKind.VALUE));
        assertThat(archeClass.name(), is("Money"));
        assertThat(archeClass.span(), is(new SourceSpan("money.bounded", 1, 1)));
        assertThat(archeClass.operations(), hasSize(1));
        assertThat(archeClass.operations().getFirst().kind(), is(ArcheOperationKind.FUNCTION));
        assertThat(archeClass.operations().getFirst().name(), is("format"));
        assertThat(archeClass.operations().getFirst().span(), is(new SourceSpan("money.bounded", 2, 3)));
    }
}
