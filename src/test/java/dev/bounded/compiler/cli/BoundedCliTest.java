package dev.bounded.compiler.cli;

import dev.bounded.compiler.api.CompilationRequest;
import dev.bounded.compiler.api.CompilationResult;
import dev.bounded.compiler.api.CompileProgram;
import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.api.write.OutputTarget;
import dev.bounded.compiler.api.write.CompiledClass;
import dev.bounded.compiler.api.generate.ClassName;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.diagnostics.Diagnostic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.SYNTAX_ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Command-line adapter")
class BoundedCliTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();
    private CompileProgram compiler = request -> CompilationResult.success(List.of(new CompiledClass(new ClassName("Amount"), Path.of("classes"))));

    @TempDir
    Path temporaryDirectory;
    private Path sourcePath;
    private Path outputRoot;

    @BeforeEach
    void createSourceFile() throws IOException {
        sourcePath = Files.writeString(temporaryDirectory.resolve("Amount.bounded"), "value Amount {}", UTF_8);
        outputRoot = temporaryDirectory.resolve("output");
    }

    private static Stream<Arguments> invalidArguments() {
        return Stream.of(
                Arguments.of("no arguments", new String[]{}),
                Arguments.of("only a source", new String[]{"Amount.bounded"}),
                Arguments.of("an extra argument", new String[]{"Amount.bounded", "output", "extra"}));
    }

    @DisplayName("Invalid arguments report usage without compiling")
    @ParameterizedTest(name = "Reports usage for {0}")
    @MethodSource("invalidArguments")
    void reportsUsage(String description, String[] arguments) throws IOException {
        compiler = request -> fail("Invalid arguments must not invoke compilation");

        assertThat(cli().run(arguments), is(2));
        assertThat(errors.toString(UTF_8), is("Usage: bounded <source.bounded> <output-directory>" + System.lineSeparator()));
        assertThat(output.toString(UTF_8), is(emptyString()));
    }

    @Test
    @DisplayName("Passes the source path and exact UTF-8 content to the compiler")
    void readsSourceAsUtf8() throws IOException {
        var content = "café\nλ";
        Files.writeString(sourcePath, content, UTF_8);
        var received = new AtomicReference<CompilationRequest>();
        compiler = request -> {
            received.set(request);
            return CompilationResult.success(List.of(new CompiledClass(new ClassName("Amount"), Path.of("classes"))));
        };

        run();

        assertThat(received.get().sourceFile(), is(new SourceFile(sourcePath.toString(), content)));
    }

    @Test
    @DisplayName("Passes the requested output directory to the compiler")
    void passesOutputTarget() throws IOException {
        var received = new AtomicReference<CompilationRequest>();
        compiler = request -> {
            received.set(request);
            return CompilationResult.success(List.of(new CompiledClass(new ClassName("Amount"), Path.of("classes"))));
        };

        run();

        assertThat(received.get().outputTarget(), is(new OutputTarget(outputRoot)));
    }

    @Test
    @DisplayName("Successful compilation returns exit status zero")
    void reportsSuccess() throws IOException {
        assertThat(run(), is(0));
    }

    @Test
    @DisplayName("Prints every generated path to standard output in result order")
    void printsGeneratedFiles() throws IOException {
        var first = outputRoot.resolve("Amount.class");
        var second = outputRoot.resolve("AmountHelper.class");
        compiler = request -> CompilationResult.success(List.of(
                new CompiledClass(new ClassName("Amount"), outputRoot),
                new CompiledClass(new ClassName("AmountHelper"), outputRoot)));

        run();

        assertThat(output.toString(UTF_8), is(
                "Generated " + first.toAbsolutePath() + System.lineSeparator()
                        + "Generated " + second.toAbsolutePath() + System.lineSeparator()));
        assertThat(errors.toString(UTF_8), is(emptyString()));
    }

    @Test
    @DisplayName("Compilation diagnostics return exit status one")
    void reportsCompilationFailure() throws IOException {
        compiler = request -> CompilationResult.failure(List.of(diagnostic()));

        assertThat(run(), is(1));
    }

    @Test
    @DisplayName("Prints each diagnostic using its own source path in result order")
    void printsDiagnostics() throws IOException {
        var second = new Diagnostic(SYNTAX_ERROR, new SourceSpan("dependency.bounded", 4, 1), "Missing closing brace");
        compiler = request -> CompilationResult.failure(List.of(diagnostic(), second));

        run();

        assertThat(errors.toString(UTF_8), is(
                sourcePath + ":BND0001:2:3 Missing semicolon" + System.lineSeparator()
                        + "dependency.bounded:BND0001:4:1 Missing closing brace" + System.lineSeparator()));
        assertThat(output.toString(UTF_8), is(emptyString()));
    }

    @Test
    @DisplayName("An unreadable source fails before invoking compilation")
    void propagatesSourceReadFailure() {
        compiler = request -> fail("An unreadable source must not invoke compilation");

        assertThrows(NoSuchFileException.class,
                () -> cli().run(temporaryDirectory.resolve("missing.bounded").toString(), outputRoot.toString()));
    }

    @Test
    @DisplayName("Compilation I/O failures propagate to the entry point")
    void propagatesCompilationFailure() {
        var failure = new IOException("Cannot write class file");
        compiler = request -> {
            throw failure;
        };

        assertThat(assertThrows(IOException.class, this::run), sameInstance(failure));
    }

    private static Stream<Arguments> missingDependencies() {
        CompileProgram compiler = request -> fail("Construction must not invoke compilation");
        var stream = new PrintStream(OutputStream.nullOutputStream(), false, UTF_8);
        return Stream.of(
                Arguments.of("compiler", null, stream, stream),
                Arguments.of("output stream", compiler, null, stream),
                Arguments.of("error stream", compiler, stream, null));
    }

    @DisplayName("The CLI requires a compiler and both output streams")
    @ParameterizedTest(name = "Rejects a missing {0}")
    @MethodSource("missingDependencies")
    void rejectsMissingDependencies(String description, CompileProgram compiler, PrintStream output, PrintStream errors) {
        assertThrows(NullPointerException.class, () -> new BoundedCli(compiler, output, errors));
    }

    private int run() throws IOException {
        return cli().run(sourcePath.toString(), outputRoot.toString());
    }

    private BoundedCli cli() {
        return new BoundedCli(compiler, new PrintStream(output, true, UTF_8), new PrintStream(errors, true, UTF_8));
    }

    private Diagnostic diagnostic() {
        return new Diagnostic(SYNTAX_ERROR, new SourceSpan(sourcePath.toString(), 2, 3), "Missing semicolon");
    }
}
