package dev.bounded.compiler.cli;

import dev.bounded.compiler.api.CompileProgram;

public final class ProductionCompiler {
    private ProductionCompiler() {}

    public static CompileProgram create() {
        return Main.compiler();
    }
}
