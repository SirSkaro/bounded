package dev.bounded.compiler.api.generate;

import dev.bounded.compiler.ast.AnalyzedProgram;

@FunctionalInterface
public interface CodeGenerator {
    GenerationResult generate(AnalyzedProgram program);
}
