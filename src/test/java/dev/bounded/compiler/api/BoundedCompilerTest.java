package dev.bounded.compiler.api;

import dev.bounded.compiler.api.check.CheckResult;
import dev.bounded.compiler.api.check.SemanticChecker;
import dev.bounded.compiler.api.generate.CodeGenerator;
import dev.bounded.compiler.api.generate.GeneratedClass;
import dev.bounded.compiler.api.generate.GeneratedProgram;
import dev.bounded.compiler.api.generate.ClassName;
import dev.bounded.compiler.api.write.CompiledClass;
import dev.bounded.compiler.ast.AnalyzedProgram;
import dev.bounded.compiler.api.generate.GenerationResult;
import dev.bounded.compiler.api.parse.ParseResult;
import dev.bounded.compiler.api.parse.Parser;
import dev.bounded.compiler.api.write.ArtifactWriter;
import dev.bounded.compiler.api.write.OutputTarget;
import dev.bounded.compiler.api.write.WriteResult;
import dev.bounded.compiler.ast.ArcheClassDeclaration;
import dev.bounded.compiler.ast.BoundedProgram;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static dev.bounded.compiler.ast.ArcheClassKind.VALUE;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.EFFECT_NOT_ALLOWED;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.JVM_BACKEND_FAILURE;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.SYNTAX_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Compiler application policy")
class BoundedCompilerTest {
    private final BoundedProgram program = new BoundedProgram(
            new ArcheClassDeclaration(VALUE, "Amount", List.of(), new SourceSpan(1, 1)));
    private final GeneratedClass generatedClass = new GeneratedClass("example.Amount", new byte[]{1, 2, 3});
    private final AnalyzedProgram analyzed = new AnalyzedProgram(program);
    private final GeneratedProgram generatedProgram = new GeneratedProgram(List.of(generatedClass));
    private final List<CompiledClass> artifacts = List.of(
            new CompiledClass(new ClassName("example.Amount"), Path.of("classes")),
            new CompiledClass(new ClassName("example.AmountHelper"), Path.of("classes")));
    private final CompilationRequest request = new CompilationRequest(
            new SourceFile("Amount.bounded", "value Amount {}"), new OutputTarget(Path.of("output")));

    private Parser parser = source -> ParseResult.validResult(program);
    private SemanticChecker checker = parsed -> CheckResult.validResult(analyzed);
    private CodeGenerator generator = checked -> GenerationResult.validResult(generatedProgram);
    private ArtifactWriter writer = (generated, target) -> new WriteResult(artifacts);

    @Nested
    @DisplayName("Successful compilation")
    class SuccessfulCompilation {
        @Test
        @DisplayName("Runs parsing, checking, generation, and writing in order")
        void runsStagesInOrder() throws IOException {
            var stages = new ArrayList<String>();
            parser = source -> {
                stages.add("parse");
                return ParseResult.validResult(program);
            };
            checker = parsed -> {
                stages.add("check");
                return CheckResult.validResult(analyzed);
            };
            generator = checked -> {
                stages.add("generate");
                return GenerationResult.validResult(generatedProgram);
            };
            writer = (generated, target) -> {
                stages.add("write");
                return new WriteResult(artifacts);
            };

            compile();

            assertThat(stages, contains("parse", "check", "generate", "write"));
        }

        @Test
        @DisplayName("Parses the requested source")
        void passesSourceToParser() throws IOException {
            var received = new AtomicReference<SourceFile>();
            parser = source -> {
                received.set(source);
                return ParseResult.validResult(program);
            };

            compile();

            assertThat(received.get(), sameInstance(request.sourceFile()));
        }

        @Test
        @DisplayName("Checks the program returned by the parser")
        void passesProgramToChecker() throws IOException {
            var received = new AtomicReference<BoundedProgram>();
            checker = parsed -> {
                received.set(parsed);
                return CheckResult.validResult(analyzed);
            };

            compile();

            assertThat(received.get(), sameInstance(program));
        }

        @Test
        @DisplayName("Generates code from the checked program")
        void passesProgramToGenerator() throws IOException {
            var received = new AtomicReference<AnalyzedProgram>();
            generator = checked -> {
                received.set(checked);
                return GenerationResult.validResult(generatedProgram);
            };

            compile();

            assertThat(received.get(), sameInstance(analyzed));
        }

        @Test
        @DisplayName("Writes every class returned by the generator")
        void passesClassToWriter() throws IOException {
            var received = new AtomicReference<GeneratedProgram>();
            writer = (generated, target) -> {
                received.set(generated);
                return new WriteResult(artifacts);
            };

            compile();

            assertThat(received.get(), sameInstance(generatedProgram));
        }

        @Test
        @DisplayName("Writes to the requested output target")
        void passesOutputTargetToWriter() throws IOException {
            var received = new AtomicReference<OutputTarget>();
            writer = (generated, target) -> {
                received.set(target);
                return new WriteResult(artifacts);
            };

            compile();

            assertThat(received.get(), sameInstance(request.outputTarget()));
        }

        @Test
        @DisplayName("Returns all artifacts reported by the writer without diagnostics")
        void returnsWrittenClassFiles() throws IOException {
            var result = compile();
            assertThat(result.artifacts(), is(artifacts));
            assertThat(result.diagnostics(), is(List.of()));
        }
    }

    @Nested
    @DisplayName("Failed compilation")
    class FailedCompilation {
        @Test
        @DisplayName("Syntax diagnostics stop compilation before checking")
        void stopsAfterSyntaxErrors() throws IOException {
            var diagnostics = List.of(
                    new Diagnostic(SYNTAX_ERROR, new SourceSpan(1, 1), "First syntax error"),
                    new Diagnostic(SYNTAX_ERROR, new SourceSpan(2, 3), "Second syntax error"));
            parser = source -> ParseResult.invalidResult(diagnostics);
            checker = parsed -> fail("Checking must not run after a parse failure");
            generator = checked -> fail("Generation must not run after a parse failure");
            writer = (generated, target) -> fail("Writing must not run after a parse failure");

            var result = compile();
            assertThat(result.diagnostics(), is(diagnostics));
            assertThat(result.artifacts(), is(List.of()));
        }

        @Test
        @DisplayName("Semantic diagnostics stop compilation before generation")
        void stopsAfterSemanticErrors() throws IOException {
            var diagnostics = List.of(
                    new Diagnostic(EFFECT_NOT_ALLOWED, new SourceSpan(2, 3), "First effect error"),
                    new Diagnostic(EFFECT_NOT_ALLOWED, new SourceSpan(3, 3), "Second effect error"));
            checker = parsed -> CheckResult.invalidResult(diagnostics);
            generator = checked -> fail("Generation must not run after a semantic failure");
            writer = (generated, target) -> fail("Writing must not run after a semantic failure");

            var result = compile();
            assertThat(result.diagnostics(), is(diagnostics));
            assertThat(result.artifacts(), is(List.of()));
        }

        @Test
        @DisplayName("Generation diagnostics stop compilation before writing")
        void stopsAfterGenerationErrors() throws IOException {
            var diagnostics = List.of(
                    new Diagnostic(JVM_BACKEND_FAILURE, new SourceSpan(1, 1), "First generation error"),
                    new Diagnostic(JVM_BACKEND_FAILURE, new SourceSpan(1, 1), "Second generation error"));
            generator = checked -> GenerationResult.invalidResult(diagnostics);
            writer = (generated, target) -> fail("Writing must not run after a generation failure");

            var result = compile();
            assertThat(result.diagnostics(), is(diagnostics));
            assertThat(result.artifacts(), is(List.of()));
        }

        @Test
        @DisplayName("Writing failures propagate to the caller")
        void propagatesWritingFailures() {
            var failure = new IOException("Cannot write output");
            writer = (generated, target) -> {
                throw failure;
            };

            assertThat(assertThrows(IOException.class, BoundedCompilerTest.this::compile), sameInstance(failure));
        }

        @Test
        @DisplayName("Unexpected implementation exceptions propagate to the caller")
        void propagatesUnexpectedFailures() {
            var failure = new IllegalStateException("Unexpected generator failure");
            generator = checked -> {
                throw failure;
            };

            assertThat(assertThrows(IllegalStateException.class, BoundedCompilerTest.this::compile), sameInstance(failure));
        }
    }

    private static Stream<Arguments> missingPorts() {
        Parser parser = source -> fail("Construction must not invoke the parser");
        SemanticChecker checker = program -> fail("Construction must not invoke the checker");
        CodeGenerator generator = program -> fail("Construction must not invoke the generator");
        ArtifactWriter writer = (generated, target) -> fail("Construction must not invoke the writer");
        return Stream.of(
                Arguments.of("parser", null, checker, generator, writer),
                Arguments.of("checker", parser, null, generator, writer),
                Arguments.of("generator", parser, checker, null, writer),
                Arguments.of("writer", parser, checker, generator, null));
    }

    @DisplayName("Every compiler port must be supplied")
    @ParameterizedTest(name = "Rejects a missing {0}")
    @MethodSource("missingPorts")
    void rejectsMissingPorts(String description, Parser parser, SemanticChecker checker,
                             CodeGenerator generator, ArtifactWriter writer) {
        assertThrows(NullPointerException.class, () -> new BoundedCompiler(parser, checker, generator, writer));
    }

    private CompilationResult compile() throws IOException {
        return new BoundedCompiler(parser, checker, generator, writer).compile(request);
    }
}
