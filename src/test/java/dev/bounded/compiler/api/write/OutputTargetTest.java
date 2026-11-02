package dev.bounded.compiler.api.write;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Output target invariants")
class OutputTargetTest {
    @Test
    @DisplayName("An output target requires a root directory")
    void rejectsMissingRoot() {
        assertThrows(NullPointerException.class, () -> new OutputTarget(null));
    }
}
