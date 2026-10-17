package dev.bounded.compiler.ast;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public enum ArcheOperationKind {
    FUNCTION(EffectGrade.FUNCTION, "Function"),
    QUERY(EffectGrade.QUERY, "Query");

    private final EffectGrade effectGrade;
    private final String label;

    ArcheOperationKind(EffectGrade effectGrade, String label) {
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

    public EffectGrade effectGrade() {
        return effectGrade;
    }

    public String label() {
        return label;
    }
}
