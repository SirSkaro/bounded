package dev.bounded.compiler.api;

import dev.bounded.compiler.backend.JavaClassCompiler;
import dev.bounded.compiler.backend.JavaSourceBackend;
import dev.bounded.compiler.parse.BoundedParserFacade;
import dev.bounded.compiler.semantics.EffectChecker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class BoundedCompiler {
    private final BoundedParserFacade parser = new BoundedParserFacade();
    private final EffectChecker effectChecker = new EffectChecker();
    private final JavaSourceBackend backend = new JavaSourceBackend();
    private final JavaClassCompiler javaClassCompiler = new JavaClassCompiler();

    public CompilationResult compile(SourceFile sourceFile, Path outputRoot) throws IOException {
        var parseResult = parser.parse(sourceFile);
        if (!parseResult.isValid()) {
            return new CompilationResult(parseResult.diagnostics(), List.of(), List.of());
        }

        var program = parseResult.program();
        var effectCheckResult = effectChecker.check(program);
        if (!effectCheckResult.isValid()) {
            return new CompilationResult(effectCheckResult.diagnostics(), List.of(), List.of());
        }

        var generatedSource = backend.generate(program);
        var javaCompilation = javaClassCompiler.compile(generatedSource, outputRoot);
        return new CompilationResult(
                javaCompilation.diagnostics(),
                javaCompilation.generatedSourceFiles(),
                javaCompilation.generatedClassFiles());
    }
}
