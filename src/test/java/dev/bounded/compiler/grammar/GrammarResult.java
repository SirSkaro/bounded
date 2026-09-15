package dev.bounded.compiler.grammar;

import java.util.List;

record GrammarResult(String source, BoundedParser.ProgramContext tree, List<String> errors)
        implements GrammarParseResult {
    ArcheClassFacade archeClass() {
        return new ArcheClassFacade(tree.archeClass());
    }
}
