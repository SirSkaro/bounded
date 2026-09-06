package dev.bounded.compiler.cli;

import dev.bounded.compiler.api.BoundedCompiler;
import dev.bounded.compiler.api.SourceFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BoundedCli {
    private BoundedCli() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: bounded <source.bounded> <output-directory>");
            System.exit(2);
        }

        var sourcePath = Path.of(args[0]);
        var outputRoot = Path.of(args[1]);
        var sourceFile = new SourceFile(
                sourcePath.toString(),
                Files.readString(sourcePath, StandardCharsets.UTF_8));
        var result = new BoundedCompiler().compile(sourceFile, outputRoot);

        for (var diagnostic : result.diagnostics()) {
            System.err.println(sourcePath + ":" + diagnostic.format());
        }

        for (var classFile : result.generatedClassFiles()) {
            System.out.println("Generated " + classFile.toAbsolutePath());
        }

        if (!result.succeeded()) {
            System.exit(1);
        }
    }
}
