package dev.bounded.compiler.ast;

import java.util.Locale;

public enum ArcheOperationKind {
    FUNCTION(EffectGrade.FUNCTION),
    QUERY(EffectGrade.QUERY);

    private final EffectGrade effectGrade;

    ArcheOperationKind(EffectGrade effectGrade) {
        this.effectGrade = effectGrade;
    }

    public static ArcheOperationKind fromKeyword(String keyword) {
        return valueOf(keyword.toUpperCase(Locale.ROOT));
    }

    public EffectGrade effectGrade() {
        return effectGrade;
    }
}
