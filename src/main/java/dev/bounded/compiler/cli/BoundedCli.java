package dev.bounded.compiler.cli;

import dev.bounded.compiler.api.CompilationRequest;
import dev.bounded.compiler.api.CompileProgram;
import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.api.write.OutputTarget;
import dev.bounded.compiler.diagnostics.TextDiagnosticFormatter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

public final class BoundedCli {
    private final CompileProgram compiler;
    private final PrintStream output;
    private final PrintStream errors;

    public BoundedCli(CompileProgram compiler, PrintStream output, PrintStream errors) {
        this.compiler = requireNonNull(compiler, "compiler");
        this.output = requireNonNull(output, "output");
        this.errors = requireNonNull(errors, "errors");
    }

    public int run(String... args) throws IOException {
        if (args.length != 2) {
            errors.println("Usage: bounded <source.bounded> <output-directory>");
            return 2;
        }

        var sourcePath = Path.of(args[0]);
        var outputRoot = Path.of(args[1]);
        var sourceFile = new SourceFile(
                sourcePath.toString(),
                Files.readString(sourcePath, StandardCharsets.UTF_8));
        var result = compiler.compile(new CompilationRequest(sourceFile, new OutputTarget(outputRoot)));

        for (var diagnostic : result.diagnostics()) {
            errors.println(TextDiagnosticFormatter.format(diagnostic));
        }

        for (var artifact : result.artifacts()) {
            output.println("Generated " + artifact.classFile().toAbsolutePath());
        }

        return result.succeeded() ? 0 : 1;
    }
}
