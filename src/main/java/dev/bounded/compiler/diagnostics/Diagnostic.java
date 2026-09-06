package dev.bounded.compiler.diagnostics;

import dev.bounded.compiler.ast.SourceSpan;

import java.util.Objects;

public record Diagnostic(
        DiagnosticCode code,
        SourceSpan span,
        String message
) {
    public Diagnostic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(span, "span");
        Objects.requireNonNull(message, "message");
    }

    public String format() {
        return "%s:%d:%d %s".formatted(
                code.externalCode(), span.line(), span.column(), message);
    }
}
