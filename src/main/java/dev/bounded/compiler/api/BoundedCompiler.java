package dev.bounded.compiler.api;

import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.backend.ClassFileWriter;
import dev.bounded.compiler.backend.GeneratedClass;
import dev.bounded.compiler.backend.JvmBackend;
import dev.bounded.compiler.diagnostics.Diagnostic;
import dev.bounded.compiler.parse.BoundedParserFacade;
import dev.bounded.compiler.semantics.EffectChecker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.JVM_BACKEND_FAILURE;

public final class BoundedCompiler {
    private final BoundedParserFacade parser = new BoundedParserFacade();
    private final EffectChecker effectChecker = new EffectChecker();
    private final JvmBackend backend = new JvmBackend();
    private final ClassFileWriter classFileWriter = new ClassFileWriter();

    public CompilationResult compile(SourceFile sourceFile, Path outputRoot) throws IOException {
        var parseResult = parser.parse(sourceFile);
        if (!parseResult.isValid()) {
            return new CompilationResult(parseResult.diagnostics(), List.of());
        }

        var program = parseResult.program();
        var effectCheckResult = effectChecker.check(program);
        if (!effectCheckResult.isValid()) {
            return new CompilationResult(effectCheckResult.diagnostics(), List.of());
        }

        GeneratedClass generatedClass;
        try {
            generatedClass = backend.generate(program);
        } catch (IllegalArgumentException exception) {
            return backendFailure(program, exception);
        }

        var classFile = classFileWriter.write(generatedClass, outputRoot);
        return new CompilationResult(List.of(), List.of(classFile));
    }

    private CompilationResult backendFailure(BoundedProgram program, IllegalArgumentException exception) {
        var diagnostic = new Diagnostic(
                JVM_BACKEND_FAILURE,
                program.archeClass().span(),
                "Could not generate JVM bytecode: " + exception.getMessage());
        return new CompilationResult(List.of(diagnostic), List.of());
    }
}
