package dev.bounded.compiler.cli;

import dev.bounded.compiler.acceptance.CompilerDriver;
import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.ast.SourceSpan;
import dev.bounded.compiler.locale.LocaleSensitiveTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static dev.bounded.compiler.acceptance.AcceptanceSources.expectedDiagnostics;
import static dev.bounded.compiler.acceptance.AcceptanceSources.fixture;
import static dev.bounded.compiler.acceptance.CompilationMatchers.accepted;
import static dev.bounded.compiler.acceptance.CompilationMatchers.rejectedWithCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Isolated
@DisplayName("Compiler acceptance through the production composition root")
class FixtureCompilationTest {
    @TempDir
    Path temporaryDirectory;
    CompilerDriver compiler;

    @BeforeEach
    void createCompilerDriver() {
        compiler = new CompilerDriver(temporaryDirectory);
    }

    @Nested
    @DisplayName("Basic ArcheOperation declarations")
    class ValidOperationDeclarations {

        @LocaleSensitiveTest
        @DisplayName("A value with ArcheOperations up to its ceiling effect grade compiles and appears in declaration metadata")
        void compilesAValueAtItsEffectCeiling() throws Exception {
            var compilation = compiler.compile("""
                value Money {
                  function format;
                }
            """);
            assertThat(compilation, accepted());
            assertThat(compilation.result().artifacts(), hasSize(1));

            try(var program = compilation.load()) {
                var archeClass = program.archeClass();
                assertThat(archeClass.kind(), is("VALUE"));
                assertThat(archeClass.operations(), is(Map.of(
                    "format", "FUNCTION"
                )));
            }
        }

        @LocaleSensitiveTest
        @DisplayName("An entity with ArcheOperations up to its ceiling effect grade compiles and appears in declaration metadata")
        void compilesAnEntityThroughItsEffectCeiling() throws Exception {
            var compilation = compiler.compile("""
            entity Account {
                function format;
                query amount;
                command deposit;
            }
            """);

            assertThat(compilation, accepted());

            try (var program = compilation.load()) {
                var archeClass = program.archeClass();
                assertThat(archeClass.kind(), is("ENTITY"));
                assertThat(archeClass.operations(), is(Map.of(
                "format", "FUNCTION",
                "amount", "QUERY",
                "deposit", "COMMAND"
                )));
            }
        }

        @LocaleSensitiveTest
        @DisplayName("A use case with ArcheOperations up to its ceiling effect grade compiles and appears in declaration metadata")
        void compilesUseCaseThroughItsEffectCeiling() throws Exception {
            var compilation = compiler.compile("""
            usecase AccountTransfer {
                function format;
                query hasFunds;
                command transfer;
                action commit;
            }
            """);

            assertThat(compilation, accepted());

            try (var program = compilation.load()) {
                var archeClass = program.archeClass();
                assertThat(archeClass.kind(), is("USECASE"));
                assertThat(archeClass.operations(), is(Map.of(
                    "format", "FUNCTION",
                    "hasFunds", "QUERY",
                    "transfer", "COMMAND",
                    "commit", "ACTION"
                )));
            }
        }

        @LocaleSensitiveTest
        @DisplayName("A message compiles with no operation metadata")
        void compilesAMessageWithoutOperations() throws Exception {
            var compilation = compiler.compile("""
                message TransferRequest {}
            """);

            assertThat(compilation, accepted());
            assertThat(compilation.result().artifacts(), hasSize(1));

            try (var program = compilation.load()) {
                var archeClass = program.archeClass();
                assertThat(archeClass.kind(), is("MESSAGE"));
                assertThat(archeClass.operations(), is(Map.of()));
            }
        }
    }

    @Nested
    @DisplayName("Invalid semantics")
    class SemanticRejections {
        @Test
        @DisplayName("A value with an ArcheOperation exceeding its ceiling effect grade produces the specified diagnostic")
        void rejectsAnOperationAboveTheValueCeiling() throws IOException {
            var compilation = compiler.compile(fixture("cases/invalid/value-query.bounded"));
            assertThat(compilation, rejectedWithCode("BND1001"));
            assertThat(compilation.formattedDiagnostics(), is(expectedDiagnostics("cases/invalid/value-query.diag")));
        }

        @Test
        @DisplayName("An entity cannot declare an action above its effect ceiling")
        void rejectsAnOperationAboveTheEntityCeiling() throws IOException {
            var compilation = compiler.compile("""
            entity Account {
                action commit;
            }
            """);

            assertThat(compilation, rejectedWithCode("BND1001"));
        }

        @Test
        @DisplayName("A duplicate operation is rejected before generation")
        void rejectsDuplicateOperations() throws IOException {
            var compilation = compiler.compile(fixture("cases/invalid/duplicate-operation.bounded"));
            assertThat(compilation, rejectedWithCode("BND1002"));
            assertThat(compilation.formattedDiagnostics(), is(expectedDiagnostics("cases/invalid/duplicate-operation.diag")));
        }

        @Test
        @DisplayName("A message cannot declare even a function")
        void rejectsEvenAFunctionInAMessage() throws IOException {
            var compilation = compiler.compile(fixture("cases/invalid/message-function.bounded"));
            assertThat(compilation, rejectedWithCode("BND1001"));
            assertThat(compilation.formattedDiagnostics(), is(expectedDiagnostics("cases/invalid/message-function.diag")));
        }

    }

    @Nested
    @DisplayName("Syntax Rejection")
    class SyntaxRejection {
        @DisplayName("Syntax errors produce diagnostics without creating output")
        @ParameterizedTest(name = "Rejects invalid source [{0}]")
        @ValueSource(strings = {"value Amount {", "value Amount { fn format; }"})
        void stopsAfterSyntaxErrors(String source) throws IOException {
            assertThat(compiler.compile(source), rejectedWithCode("BND0001"));
        }
    }

    @Nested
    @DisplayName("Compiler failures")
    class CompilerFailures {
        @Test
        @DisplayName("Bytecode generation failures retain their source location without artifacts")
        void reportsBytecodeGenerationFailure() throws IOException {
            var source = new SourceFile("oversized.bounded", "\n  value " + "A".repeat(65_536) + " {}");
            var compilation = compiler.compile(source);
            assertThat(compilation, rejectedWithCode("BND9001"));
            var diagnostic = compilation.result().diagnostics().getFirst();
            assertThat(diagnostic.span(), is(new SourceSpan("oversized.bounded", 2, 3)));
            assertThat(diagnostic.message(), startsWith("Could not generate JVM bytecode: "));
        }

        @Test
        @DisplayName("Filesystem failures propagate through the configured compiler")
        void propagatesFilesystemFailures() throws IOException {
            var outputRoot = Files.writeString(temporaryDirectory.resolve("occupied"), "occupied");
            var source = new SourceFile("Amount.bounded", "value Amount {}");
            assertThrows(IOException.class, () -> compiler.compileTo(source, outputRoot));
        }
    }

}
