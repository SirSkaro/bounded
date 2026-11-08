package dev.bounded.compiler.parse;

import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.ArcheClassKind;
import dev.bounded.compiler.ast.ArcheOperationDeclaration;
import dev.bounded.compiler.ast.ArcheOperationKind;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.grammar.BoundedParser.ArcheClassContext;
import dev.bounded.compiler.grammar.BoundedParser.ArcheOperationContext;
import dev.bounded.compiler.grammar.BoundedParser.ProgramContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

final class BoundedAstMapper {

    BoundedProgram map(ProgramContext programContext, String sourceName) {
        var archeClassDeclaration = toDeclaration(programContext.archeClass(), sourceName);

        return new BoundedProgram(archeClassDeclaration);
    }

    private ArcheClassDeclaration toDeclaration(ArcheClassContext archeClassContext, String sourceName) {
        return new ArcheClassDeclaration(
            ArcheClassKind.fromKeyword(archeClassContext.archeClassKind().getText()),
            archeClassContext.IDENTIFIER().getText(),
            toDeclarations(archeClassContext.archeOperation(), sourceName),
            spanOf(archeClassContext.getStart(), sourceName)
        );
    }

    private List<ArcheOperationDeclaration> toDeclarations(List<ArcheOperationContext> operationContexts, String sourceName) {
        return operationContexts.stream()
                .map(context -> toDeclaration(context, sourceName))
                .toList();
    }

    private SourceSpan spanOf(Token token, String sourceName) {
        return new SourceSpan(sourceName, token.getLine(), token.getCharPositionInLine() + 1);
    }

    private ArcheOperationDeclaration toDeclaration(ArcheOperationContext context, String sourceName) {
        return new ArcheOperationDeclaration(
            ArcheOperationKind.fromKeyword(context.archeOperationKind().getText()),
            context.IDENTIFIER().getText(),
            spanOf(context.getStart(), sourceName)
        );
    }
}
