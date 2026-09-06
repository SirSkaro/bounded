package dev.bounded.compiler.ast;

public enum ArcheOperationKind {
    FUNCTION(EffectGrade.FUNCTION),
    QUERY(EffectGrade.QUERY);

    private final EffectGrade effectGrade;

    ArcheOperationKind(EffectGrade effectGrade) {
        this.effectGrade = effectGrade;
    }

    public EffectGrade effectGrade() {
        return effectGrade;
    }
}
