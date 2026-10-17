package dev.bounded.compiler.diagnostics;

import dev.bounded.compiler.ast.SourceSpan;

import static java.util.Objects.requireNonNull;

public record Diagnostic(
        DiagnosticCode code,
        SourceSpan span,
        String message
) {
    public Diagnostic {
        requireNonNull(code, "code");
        requireNonNull(span, "span");
        requireNonNull(message, "message");
    }

    public String format() {
        return code.externalCode() + ":" + span.line() + ":" + span.column() + " " + message;
    }
}
