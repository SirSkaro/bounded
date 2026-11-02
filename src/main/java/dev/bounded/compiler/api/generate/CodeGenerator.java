package dev.bounded.compiler.api.generate;

import dev.bounded.compiler.ast.BoundedProgram;

@FunctionalInterface
public interface CodeGenerator {
    GenerationResult generate(BoundedProgram program);
}
