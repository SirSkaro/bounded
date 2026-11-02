package dev.bounded.compiler.cli;

import dev.bounded.compiler.api.BoundedCompiler;
import dev.bounded.compiler.api.CompileProgram;
import dev.bounded.compiler.backend.ClassFileWriter;
import dev.bounded.compiler.backend.JvmBackend;
import dev.bounded.compiler.parse.BoundedParserFacade;
import dev.bounded.compiler.semantics.EffectChecker;

import java.io.IOException;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws IOException {
        var cli = new BoundedCli(compiler(), System.out, System.err);
        System.exit(cli.run(args));
    }

    static CompileProgram compiler() {
        return new BoundedCompiler(
                new BoundedParserFacade(),
                new EffectChecker(),
                new JvmBackend(),
                new ClassFileWriter());
    }
}
