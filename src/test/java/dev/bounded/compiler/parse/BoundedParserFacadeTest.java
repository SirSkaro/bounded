package dev.bounded.compiler.parse;

import dev.bounded.compiler.locale.LocaleSensitiveTest;
import org.junit.jupiter.api.parallel.Isolated;

import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@Isolated
class BoundedParserFacadeTest {
    private final BoundedParserFacade parser = new BoundedParserFacade();

    @LocaleSensitiveTest
    @DisplayName("Maps source to the AST independently of the default locale")
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
        assertThat(archeClass.span(), is(new SourceSpan(1, 1)));
        assertThat(archeClass.operations(), hasSize(1));
        assertThat(archeClass.operations().getFirst().kind(), is(ArcheOperationKind.FUNCTION));
        assertThat(archeClass.operations().getFirst().name(), is("format"));
        assertThat(archeClass.operations().getFirst().span(), is(new SourceSpan(2, 3)));
    }
}
