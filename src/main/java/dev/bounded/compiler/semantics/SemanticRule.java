package dev.bounded.compiler.semantics;

import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.diagnostics.Diagnostic;

import java.util.List;

@FunctionalInterface
interface SemanticRule {
    List<Diagnostic> check(BoundedProgram program);
}
