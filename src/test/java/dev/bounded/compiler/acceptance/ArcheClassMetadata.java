package dev.bounded.compiler.acceptance;

import java.util.Map;

public record ArcheClassMetadata(String kind, Map<String, String> operations) {
    public ArcheClassMetadata {
        operations = Map.copyOf(operations);
    }
}
