package dev.bounded.compiler.acceptance;

import dev.bounded.compiler.api.CompilationRequest;
import dev.bounded.compiler.api.CompileProgram;
import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.api.write.OutputTarget;
import dev.bounded.compiler.cli.ProductionCompiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CompilerDriver {
    private final CompileProgram compiler = ProductionCompiler.create();
    private final Path workspace;

    public CompilerDriver(Path workspace) {
        this.workspace = workspace;
    }

    public CompilationOutcome compile(String source) throws IOException {
        return compile(new SourceFile("example.bounded", source));
    }

    public CompilationOutcome compile(SourceFile source) throws IOException {
        var output = Files.createTempDirectory(workspace, "compilation-").resolve("output");
        return compileTo(source, output);
    }

    public CompilationOutcome compileTo(SourceFile source, Path output) throws IOException {
        var result = compiler.compile(new CompilationRequest(source, new OutputTarget(output)));
        var artifactsExist = result.artifacts().stream().allMatch(artifact -> Files.isRegularFile(artifact.classFile()));
        return new CompilationOutcome(source, result, Files.exists(output),
                Files.exists(output.resolve("generated-sources")), artifactsExist);
    }
}
