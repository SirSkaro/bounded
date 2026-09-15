package dev.bounded.compiler.grammar;

final class OperationFacade {
    private final BoundedParser.ArcheOperationContext context;

    OperationFacade(BoundedParser.ArcheOperationContext context) {
        this.context = context;
    }

    String kind() {
        return context.archeOperationKind().getText();
    }

    String name() {
        return context.IDENTIFIER().getText();
    }
}
