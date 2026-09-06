package dev.bounded.compiler.parse;

import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedParserFacadeTest {
    private final BoundedParserFacade parser = new BoundedParserFacade();

    @Test
    void mapsSourceTextToTheBoundedAst() {
        var source = new SourceFile("money.bounded", """
                value Money {
                  function format;
                }
                """);

        var result = parser.parse(source);

        assertTrue(result.diagnostics().isEmpty());
        var archeClass = result.program().orElseThrow().archeClass();
        assertEquals(ArcheClassKind.VALUE, archeClass.kind());
        assertEquals("Money", archeClass.name());
        assertEquals(new SourceSpan(1, 1), archeClass.span());
        assertEquals(1, archeClass.operations().size());
        assertEquals(ArcheOperationKind.FUNCTION, archeClass.operations().getFirst().kind());
        assertEquals("format", archeClass.operations().getFirst().name());
        assertEquals(new SourceSpan(2, 3), archeClass.operations().getFirst().span());
    }
}
