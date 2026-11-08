package dev.bounded.compiler.ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class AnalyzedProgram {
    private final BoundedProgram program;
    private final Map<String, ArcheOperationDeclaration> operations;

    public AnalyzedProgram(BoundedProgram program) {
        this.program = requireNonNull(program, "program");
        this.operations = indexOperations(program.archeClass());
    }

    private Map<String, ArcheOperationDeclaration> indexOperations(ArcheClassDeclaration archeClass) {
        Map<String, ArcheOperationDeclaration> declarations = new LinkedHashMap<>();
        for (var operation : archeClass.operations()) {
            if (declarations.putIfAbsent(operation.name(), operation) != null) {
                throw new IllegalArgumentException("Duplicate operation: " + operation.name());
            }
        }
        return Collections.unmodifiableMap(declarations);
    }

    public BoundedProgram program() {
        return program;
    }

    public Map<String, ArcheOperationDeclaration> operations() {
        return operations;
    }
}
