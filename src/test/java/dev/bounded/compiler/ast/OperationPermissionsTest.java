package dev.bounded.compiler.ast;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static dev.bounded.compiler.ast.ArcheOperationKind.ACTION;
import static dev.bounded.compiler.ast.ArcheOperationKind.COMMAND;
import static dev.bounded.compiler.ast.ArcheOperationKind.QUERY;
import static dev.bounded.compiler.ast.OperationPermissions.noOperations;
import static dev.bounded.compiler.ast.OperationPermissions.through;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ArcheOperation declaration permissions")
class OperationPermissionsTest {
    @DisplayName("No operations means every operation kind is forbidden")
    @ParameterizedTest(name = "Forbids {0} when no operations are permitted")
    @EnumSource(ArcheOperationKind.class)
    void forbidsAllOperations(ArcheOperationKind kind) {
        assertThat(noOperations().allows(kind), is(false));
    }

    @Test
    @DisplayName("No permitted operations means there is no effect ceiling")
    void hasNoCeiling() {
        assertThat(noOperations().maximumOperationKind(), is(Optional.empty()));
    }

    @DisplayName("A ceiling permits exactly the operations at or below its effect grade")
    @ParameterizedTest(name = "Through {0} permits {1}: {2}")
    @CsvSource({
        "FUNCTION, FUNCTION, true",
        "FUNCTION, QUERY, false",
        "FUNCTION, COMMAND, false",
        "FUNCTION, ACTION, false",
        "QUERY, FUNCTION, true",
        "QUERY, QUERY, true",
        "QUERY, COMMAND, false",
        "QUERY, ACTION, false",
        "COMMAND, FUNCTION, true",
        "COMMAND, QUERY, true",
        "COMMAND, COMMAND, true",
        "COMMAND, ACTION, false",
        "ACTION, FUNCTION, true",
        "ACTION, QUERY, true",
        "ACTION, COMMAND, true",
        "ACTION, ACTION, true"
    })
    void permitsOperationsThroughCeiling(ArcheOperationKind ceiling, ArcheOperationKind operation, boolean expected) {
        assertThat(through(ceiling).allows(operation), is(expected));
    }

    @DisplayName("A nonempty permission policy retains its specified maximum grade")
    @ParameterizedTest(name = "Through {0} has ceiling {0}")
    @EnumSource(ArcheOperationKind.class)
    void retainsCeiling(ArcheOperationKind ceiling) {
        assertThat(through(ceiling).maximumOperationKind(), is(Optional.of(ceiling)));
    }

    @Test
    @DisplayName("Creating other policies does not change existing permissions")
    void keepsPoliciesIndependent() {
        var queries = through(QUERY);
        var actions = through(ACTION);
        var none = noOperations();

        assertThat(queries.allows(COMMAND), is(false));
        assertThat(actions.allows(COMMAND), is(true));
        assertThat(none.allows(COMMAND), is(false));
    }

    @Test
    @DisplayName("A null ceiling cannot stand for no operations")
    void rejectsNullCeiling() {
        assertThrows(NullPointerException.class, () -> through(null));
    }

    private static Stream<OperationPermissions> policies() {
        return Stream.of(noOperations(), through(ACTION));
    }

    @DisplayName("All policies require a real operation kind")
    @ParameterizedTest(name = "Policy [{index}] rejects a null operation")
    @MethodSource("policies")
    void rejectsNullOperation(OperationPermissions permissions) {
        assertThrows(NullPointerException.class, () -> permissions.allows(null));
    }
}
