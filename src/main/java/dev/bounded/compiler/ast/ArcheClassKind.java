package dev.bounded.compiler.ast;

import static dev.bounded.compiler.ast.EffectGrade.FUNCTION;
import static dev.bounded.compiler.ast.EffectGrade.QUERY;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public enum ArcheClassKind {
    VALUE(FUNCTION, "Value"),
    ENTITY(QUERY, "Entity");

    private final EffectGrade maximumEffect;
    private final String label;

    ArcheClassKind(EffectGrade maximumEffect, String label) {
        this.maximumEffect = maximumEffect;
        this.label = label;
    }

    public static ArcheClassKind fromKeyword(String keyword) {
        requireNonNull(keyword, "keyword");
        return stream(values())
                .filter(kind -> kind.name().equalsIgnoreCase(keyword))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown archeclass keyword: " + keyword));
    }

    public EffectGrade maximumEffect() {
        return maximumEffect;
    }

    public String label() {
        return label;
    }
}
