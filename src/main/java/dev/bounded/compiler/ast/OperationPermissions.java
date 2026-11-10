package dev.bounded.compiler.ast;

import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableSet;

public final class OperationPermissions {
    private final Set<ArcheOperationKind> allowedOperations;

    private OperationPermissions(Set<ArcheOperationKind> allowedOperations) {
        this.allowedOperations = Set.copyOf(allowedOperations);
    }

    public static OperationPermissions noOperations() {
        return new OperationPermissions(Set.of());
    }

    public static OperationPermissions through(ArcheOperationKind maximumOperationKind) {
        requireNonNull(maximumOperationKind, "maximumOperationKind");
        var allowed = stream(ArcheOperationKind.values())
                .filter(kind -> kind.effectGrade() <= maximumOperationKind.effectGrade())
                .collect(toUnmodifiableSet());
        return new OperationPermissions(allowed);
    }

    public boolean allows(ArcheOperationKind operationKind) {
        return allowedOperations.contains(requireNonNull(operationKind, "operationKind"));
    }

    public Optional<ArcheOperationKind> maximumOperationKind() {
        return allowedOperations.stream().max(comparingInt(ArcheOperationKind::effectGrade));
    }
}
