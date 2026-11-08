package dev.bounded.compiler.api.check;

import dev.bounded.compiler.diagnostics.TextDiagnosticFormatter;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public final class ValidCheckMatcher extends TypeSafeMatcher<CheckResult> {
    private ValidCheckMatcher() {}

    @Override
    protected boolean matchesSafely(CheckResult result) {
        return result.isValid();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a valid semantic check result");
    }

    @Override
    protected void describeMismatchSafely(CheckResult result, Description description) {
        description.appendText("had diagnostics ").appendValueList("[", ", ", "]",
            result.diagnostics().stream().map(TextDiagnosticFormatter::format).toList());
    }

    public static ValidCheckMatcher valid() {
        return new ValidCheckMatcher();
    }
}
