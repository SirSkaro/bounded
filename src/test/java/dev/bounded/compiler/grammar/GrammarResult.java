package dev.bounded.compiler.grammar;

import java.util.List;

record GrammarResult(String source, BoundedParser.ProgramContext tree, List<String> errors) {
    boolean isAccepted() {
        return errors.isEmpty();
    }

    ArcheClassFacade archeClass() {
        return new ArcheClassFacade(tree.archeClass());
    }
}
