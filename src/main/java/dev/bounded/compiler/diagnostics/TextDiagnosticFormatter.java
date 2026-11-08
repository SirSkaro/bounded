package dev.bounded.compiler.diagnostics;

public final class TextDiagnosticFormatter {
    private TextDiagnosticFormatter() {}

    public static String format(Diagnostic diagnostic) {
        var span = diagnostic.span();
        return span.sourceName() + ":" + diagnostic.code().externalCode() + ":"
                + span.line() + ":" + span.column() + " " + diagnostic.message();
    }
}
