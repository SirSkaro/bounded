package dev.bounded.compiler.semantics;

import dev.bounded.compiler.api.check.CheckResult;
import dev.bounded.compiler.api.check.SemanticChecker;
import dev.bounded.compiler.ast.AnalyzedProgram;
import dev.bounded.compiler.ast.BoundedProgram;

import java.util.List;

public final class ProgramChecker implements SemanticChecker {
    private final List<SemanticRule> rules;

    public ProgramChecker() {
        this(List.of(new DeclarationChecker(), new EffectChecker()));
    }

    ProgramChecker(List<SemanticRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public CheckResult check(BoundedProgram program) {
        var diagnostics = rules.stream()
                .flatMap(rule -> rule.check(program).stream())
                .toList();
        return diagnostics.isEmpty()
                ? CheckResult.validResult(new AnalyzedProgram(program))
                : CheckResult.invalidResult(diagnostics);
    }
}
