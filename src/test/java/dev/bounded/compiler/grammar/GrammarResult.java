package dev.bounded.compiler.grammar;

import java.util.List;

record GrammarResult(String source, BoundedParser.ProgramContext tree, List<String> errors) {
    boolean isAccepted() {
        return errors.isEmpty();
    }

    String classKind() {
        return tree.archeClass().archeClassKind().getText();
    }

    String className() {
        return tree.archeClass().IDENTIFIER().getText();
    }

    List<OperationFacade> operations() {
        return tree.archeClass().archeOperation().stream()
                .map(this::toFacade)
                .toList();
    }

    private OperationFacade toFacade(BoundedParser.ArcheOperationContext context) {
        return new OperationFacade(
                context.archeOperationKind().getText(),
                context.IDENTIFIER().getText()
        );
    }
}
