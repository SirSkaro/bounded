package dev.bounded.compiler.diagnostics;

public enum DiagnosticCode {
    SYNTAX_ERROR("BND0001"),
    EFFECT_NOT_ALLOWED("BND1001"),
    JVM_BACKEND_FAILURE("BND9001");

    private final String externalCode;

    DiagnosticCode(String externalCode) {
        this.externalCode = externalCode;
    }

    public String externalCode() {
        return externalCode;
    }
}
