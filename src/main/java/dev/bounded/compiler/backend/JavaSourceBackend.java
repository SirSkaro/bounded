package dev.bounded.compiler.backend;

import dev.bounded.compiler.ast.BoundedProgram;

public final class JavaSourceBackend {
    private static final String GENERATED_PACKAGE = "bounded.generated";

    public GeneratedJavaSource generate(BoundedProgram program) {
        var archeClass = program.archeClass();
        var generatedClassName = "Bounded_" + archeClass.name();
        var source = new StringBuilder()
                .append("package ").append(GENERATED_PACKAGE).append(";\n\n")
                .append("public final class ").append(generatedClassName).append(" {\n")
                .append("    private ").append(generatedClassName).append("() {}\n\n")
                .append("    public static String boundedArcheClassKind() {\n")
                .append("        return \"").append(archeClass.kind()).append("\";\n")
                .append("    }\n\n")
                .append("    public static java.util.Map<String, String> boundedOperations() {\n");

        if (archeClass.operations().isEmpty()) {
            source.append("        return java.util.Map.of();\n");
        } else {
            source.append("        return java.util.Map.ofEntries(\n");
            for (int index = 0; index < archeClass.operations().size(); index++) {
                var operation = archeClass.operations().get(index);
                source.append("                java.util.Map.entry(\"")
                        .append(operation.name())
                        .append("\", \"")
                        .append(operation.kind())
                        .append("\")");
                source.append(index + 1 < archeClass.operations().size() ? ",\n" : "\n");
            }
            source.append("        );\n");
        }

        source.append("    }\n")
                .append("}\n");

        return new GeneratedJavaSource(
                GENERATED_PACKAGE + "." + generatedClassName,
                source.toString());
    }
}
