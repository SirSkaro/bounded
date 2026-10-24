package dev.bounded.compiler.ast;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public enum ArcheOperationKind {
    FUNCTION(0, "Function"),
    QUERY(1, "Query");

    private final int effectGrade;
    private final String label;

    ArcheOperationKind(int effectGrade, String label) {
        this.effectGrade = effectGrade;
        this.label = label;
    }

    public static ArcheOperationKind fromKeyword(String keyword) {
        requireNonNull(keyword, "keyword");
        return stream(values())
                .filter(kind -> kind.name().equalsIgnoreCase(keyword))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown archeoperation keyword: " + keyword));
    }

    public int effectGrade() {
        return effectGrade;
    }

    public String label() {
        return label;
    }
}
