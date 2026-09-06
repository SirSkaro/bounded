package dev.bounded.compiler.ast;

public enum ArcheClassKind {
    VALUE(EffectGrade.FUNCTION),
    ENTITY(EffectGrade.QUERY);

    private final EffectGrade maximumEffect;

    ArcheClassKind(EffectGrade maximumEffect) {
        this.maximumEffect = maximumEffect;
    }

    public EffectGrade maximumEffect() {
        return maximumEffect;
    }
}
