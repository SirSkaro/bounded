package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.grammar.BoundedParser.ArcheOperationContext;
import dev.bounded.compiler.grammar.BoundedParser.ProgramContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

final class BoundedAstMapper {

    BoundedProgram map(ProgramContext programContext) {
        var archeClassContext = programContext.archeClass();
        var declaration = new ArcheClassDeclaration(
            ArcheClassKind.valueOf(archeClassContext.archeClassKind().getText().toUpperCase()),
            archeClassContext.IDENTIFIER().getText(),
            mapOperations(archeClassContext.archeOperation()),
            spanOf(archeClassContext.getStart())
        );
        return new BoundedProgram(declaration);
    }

    private List<ArcheOperationDeclaration> mapOperations(List<ArcheOperationContext> operationContexts) {
        return operationContexts.stream()
                .map(context -> new ArcheOperationDeclaration(
                        ArcheOperationKind.valueOf(
                                context.archeOperationKind().getText().toUpperCase()),
                        context.IDENTIFIER().getText(),
                        spanOf(context.getStart())))
                .toList();
    }

    private SourceSpan spanOf(Token token) {
        return new SourceSpan(token.getLine(), token.getCharPositionInLine() + 1);
    }
}
