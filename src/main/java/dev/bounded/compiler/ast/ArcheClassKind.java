package dev.bounded.compiler.ast;

import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static dev.bounded.compiler.ast.ArcheOperationKind.QUERY;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public enum ArcheClassKind {
    VALUE(FUNCTION, "Value"),
    ENTITY(QUERY, "Entity");

    private final ArcheOperationKind maximumOperationKind;
    private final String label;

    ArcheClassKind(ArcheOperationKind maximumOperationKind, String label) {
        this.maximumOperationKind = maximumOperationKind;
        this.label = label;
    }

    public static ArcheClassKind fromKeyword(String keyword) {
        requireNonNull(keyword, "keyword");
        return stream(values())
                .filter(kind -> kind.name().equalsIgnoreCase(keyword))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown archeclass keyword: " + keyword));
    }

    public boolean allows(ArcheOperationKind operationKind) {
        return operationKind.effectGrade() <= maximumOperationKind.effectGrade();
    }

    public ArcheOperationKind maximumOperationKind() {
        return maximumOperationKind;
    }

    public String label() {
        return label;
    }
}
