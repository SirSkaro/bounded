package dev.bounded.compiler.grammar;

import java.util.List;

final class ArcheClassFacade {
    private final BoundedParser.ArcheClassContext context;

    ArcheClassFacade(BoundedParser.ArcheClassContext context) {
        this.context = context;
    }

    String kind() {
        return context.archeClassKind().getText();
    }

    String name() {
        return context.IDENTIFIER().getText();
    }

    List<OperationFacade> operations() {
        return context.archeOperation().stream()
                .map(OperationFacade::new)
                .toList();
    }
}
