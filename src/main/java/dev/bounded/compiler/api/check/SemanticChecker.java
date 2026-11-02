package dev.bounded.compiler.api.check;

import dev.bounded.compiler.ast.BoundedProgram;

@FunctionalInterface
public interface SemanticChecker {
    CheckResult check(BoundedProgram program);
}
