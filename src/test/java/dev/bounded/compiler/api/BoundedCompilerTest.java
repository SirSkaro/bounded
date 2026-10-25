package dev.bounded.compiler.api;

import dev.bounded.compiler.ast.SourceSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.bounded.compiler.diagnostics.DiagnosticCode.JVM_BACKEND_FAILURE;
import static dev.bounded.compiler.diagnostics.DiagnosticCode.SYNTAX_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoundedCompilerTest {
    private final BoundedCompiler compiler = new BoundedCompiler();

    @TempDir
    Path temporaryDirectory;

    @Test
    @DisplayName("Syntax errors stop compilation before any artifacts are written")
    void stopsAfterSyntaxErrors() throws IOException {
        var outputRoot = temporaryDirectory.resolve("invalid");

        var result = compiler.compile(new SourceFile("Amount.bounded", "value Amount {"), outputRoot);

        assertThat(result.succeeded(), is(false));
        assertThat(result.diagnostics().getFirst().code(), is(SYNTAX_ERROR));
        assertThat(result.generatedClassFiles(), is(empty()));
        assertThat(Files.exists(outputRoot), is(false));
    }

    @Test
    @DisplayName("Bytecode generation failures report BND9001 at the archeclass without writing artifacts")
    void reportsBytecodeGenerationFailure() throws IOException {
        var outputRoot = temporaryDirectory.resolve("oversized");
        var oversizedName = "A".repeat(65_536);
        var source = new SourceFile("oversized.bounded", "\n  value " + oversizedName + " {}");

        var result = compiler.compile(source, outputRoot);

        assertThat(result.succeeded(), is(false));
        var diagnostic = result.diagnostics().getFirst();
        assertThat(diagnostic.code(), is(JVM_BACKEND_FAILURE));
        assertThat(diagnostic.code().externalCode(), is("BND9001"));
        assertThat(diagnostic.span(), is(new SourceSpan(2, 3)));
        assertThat(diagnostic.message(), startsWith("Could not generate JVM bytecode: "));
        assertThat(result.generatedClassFiles(), is(empty()));
        assertThat(Files.exists(outputRoot), is(false));
    }

    @Test
    @DisplayName("Filesystem failures propagate to the compiler's caller")
    void propagatesFilesystemFailures() throws IOException {
        var outputRoot = Files.writeString(temporaryDirectory.resolve("output"), "occupied");
        var source = new SourceFile("Amount.bounded", "value Amount {}");

        assertThrows(IOException.class, () -> compiler.compile(source, outputRoot));
    }
}
