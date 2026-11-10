package dev.bounded.compiler.ast;

import java.util.Optional;

import static dev.bounded.compiler.ast.ArcheOperationKind.ACTION;
import static dev.bounded.compiler.ast.ArcheOperationKind.COMMAND;
import static dev.bounded.compiler.ast.ArcheOperationKind.FUNCTION;
import static dev.bounded.compiler.ast.OperationPermissions.noOperations;
import static dev.bounded.compiler.ast.OperationPermissions.through;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public enum ArcheClassKind {
    VALUE(through(FUNCTION), "value", "VALUE", "Value"),
    ENTITY(through(COMMAND), "entity", "ENTITY", "Entity"),
    USECASE(through(ACTION), "usecase", "USECASE", "Use case"),
    MESSAGE(noOperations(), "message", "MESSAGE", "Message");

    private final OperationPermissions operationPermissions;
    private final String label;
    private final String keyword;
    private final String metadataName;

    ArcheClassKind(OperationPermissions operationPermissions, String keyword, String metadataName, String label) {
        this.operationPermissions = operationPermissions;
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
        return operationPermissions.allows(operationKind);
    }

    public Optional<ArcheOperationKind> maximumOperationKind() {
        return operationPermissions.maximumOperationKind();
    }

    public String label() {
        return label;
    }

    public String metadataName() {
        return metadataName;
    }
}
