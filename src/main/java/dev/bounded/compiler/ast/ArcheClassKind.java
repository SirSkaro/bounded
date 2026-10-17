package dev.bounded.compiler.ast;

import java.util.Locale;

import static dev.bounded.compiler.ast.EffectGrade.FUNCTION;
import static dev.bounded.compiler.ast.EffectGrade.QUERY;

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
        return valueOf(keyword.toUpperCase(Locale.ROOT));
    }

    public EffectGrade maximumEffect() {
        return maximumEffect;
    }

    public String label() {
        return label;
    }
}
