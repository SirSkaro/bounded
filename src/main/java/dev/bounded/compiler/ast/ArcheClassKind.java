package dev.bounded.compiler.ast;

import static dev.bounded.compiler.ast.ArcheOperationKind.COMMAND;
import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public enum ArcheClassKind {
    VALUE(FUNCTION, "value", "VALUE", "Value"),
    ENTITY(COMMAND, "entity", "ENTITY", "Entity");

    private final ArcheOperationKind maximumOperationKind;
    private final String label;
    private final String keyword;
    private final String metadataName;

    ArcheClassKind(ArcheOperationKind maximumOperationKind, String keyword, String metadataName, String label) {
        this.maximumOperationKind = maximumOperationKind;
        this.label = label;
        this.keyword = keyword;
        this.metadataName = metadataName;
    }

    public static ArcheClassKind fromKeyword(String keyword) {
        requireNonNull(keyword, "keyword");
        return stream(values())
                .filter(kind -> kind.keyword.equalsIgnoreCase(keyword))
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

    public String metadataName() {
        return metadataName;
    }
}
