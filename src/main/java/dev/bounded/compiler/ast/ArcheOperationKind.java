package dev.bounded.compiler.ast;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public enum ArcheOperationKind {
    FUNCTION(0, "function", "FUNCTION", "Function"),
    QUERY(1, "query", "QUERY", "Query"),
    COMMAND(2, "command", "COMMAND", "Command");

    private final int effectGrade;
    private final String label;
    private final String keyword;
    private final String metadataName;

    ArcheOperationKind(int effectGrade, String keyword, String metadataName, String label) {
        this.effectGrade = effectGrade;
        this.label = label;
        this.keyword = keyword;
        this.metadataName = metadataName;
    }

    public static ArcheOperationKind fromKeyword(String keyword) {
        requireNonNull(keyword, "keyword");
        return stream(values())
                .filter(kind -> kind.keyword.equalsIgnoreCase(keyword))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown archeoperation keyword: " + keyword));
    }

    public int effectGrade() {
        return effectGrade;
    }

    public String label() {
        return label;
    }

    public String metadataName() {
        return metadataName;
    }
}
