package dev.bounded.compiler.ast;

import java.util.Locale;

import static dev.bounded.compiler.ast.EffectGrade.FUNCTION;
import static dev.bounded.compiler.ast.EffectGrade.QUERY;

public enum ArcheClassKind {
    VALUE(FUNCTION),
    ENTITY(QUERY);

    private final EffectGrade maximumEffect;

    ArcheClassKind(EffectGrade maximumEffect) {
        this.maximumEffect = maximumEffect;
    }

    public static ArcheClassKind fromKeyword(String keyword) {
        return valueOf(keyword.toUpperCase(Locale.ROOT));
    }

    public EffectGrade maximumEffect() {
        return maximumEffect;
    }
}
