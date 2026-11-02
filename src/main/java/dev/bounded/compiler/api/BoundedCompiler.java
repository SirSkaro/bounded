package dev.bounded.compiler.api;

import dev.bounded.compiler.api.check.SemanticChecker;
import dev.bounded.compiler.api.generate.CodeGenerator;
import dev.bounded.compiler.api.parse.Parser;
import dev.bounded.compiler.api.write.ArtifactWriter;

import java.io.IOException;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class BoundedCompiler implements CompileProgram {
    private final Parser parser;
    private final SemanticChecker checker;
    private final CodeGenerator generator;
    private final ArtifactWriter writer;

    public BoundedCompiler(Parser parser, SemanticChecker checker, CodeGenerator generator, ArtifactWriter writer) {
        this.parser = requireNonNull(parser, "parser");
        this.checker = requireNonNull(checker, "checker");
        this.generator = requireNonNull(generator, "generator");
        this.writer = requireNonNull(writer, "writer");
    }

    @Override
    public CompilationResult compile(CompilationRequest request) throws IOException {
        var parseResult = parser.parse(request.sourceFile());
        if (!parseResult.isValid()) {
            return new CompilationResult(parseResult.diagnostics(), List.of());
        }

        var program = parseResult.program();
        var checkResult = checker.check(program);
        if (!checkResult.isValid()) {
            return new CompilationResult(checkResult.diagnostics(), List.of());
        }

        var generationResult = generator.generate(program);
        if (!generationResult.isValid()) {
            return new CompilationResult(generationResult.diagnostics(), List.of());
        }

        var writeResult = writer.write(generationResult.generatedClass(), request.outputTarget());
        return new CompilationResult(List.of(), writeResult.generatedClassFiles());
    }
}
