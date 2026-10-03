package dev.bounded.compiler.diagnostics;

import dev.bounded.compiler.ast.SourceSpan;

import java.util.Locale;

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
        return String.format(Locale.ROOT, "%s:%d:%d %s",
            code.externalCode(),
            span.line(),
            span.column(),
            message
        );
    }
}
