package dev.bounded.compiler.ast;

public enum EffectGrade {
    FUNCTION(0),
    QUERY(1);

    private final int rank;

    EffectGrade(int rank) {
        this.rank = rank;
    }

    public boolean allows(EffectGrade requested) {
        return requested.rank <= rank;
    }
}
