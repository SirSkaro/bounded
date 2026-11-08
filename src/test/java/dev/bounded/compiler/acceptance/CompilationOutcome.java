package dev.bounded.compiler.acceptance;

import dev.bounded.compiler.api.CompilationResult;
import dev.bounded.compiler.api.SourceFile;
import dev.bounded.compiler.diagnostics.TextDiagnosticFormatter;

import java.io.IOException;
import java.util.List;

public record CompilationOutcome(SourceFile source, CompilationResult result, boolean outputCreated,
                                 boolean generatedSourcesPresent, boolean artifactsExist) {
    public List<String> formattedDiagnostics() {
        return result.diagnostics().stream().map(TextDiagnosticFormatter::format).toList();
    }

    public CompiledProgram load() throws IOException, ClassNotFoundException {
        if (!result.succeeded()) {
            throw new AssertionError("Cannot load rejected program: " + explanation());
        }
        return new CompiledProgram(result.artifacts());
    }

    public String explanation() {
        return source.name() + "\n" + source.content() + "\nDiagnostics: " + formattedDiagnostics()
                + "\nArtifacts: " + result.artifacts() + "; files exist: " + artifactsExist
                + "; output created: " + outputCreated + "; Java sources present: " + generatedSourcesPresent;
    }
}
