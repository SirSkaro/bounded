package dev.bounded.compiler.semantics;

import dev.bounded.compiler.diagnostics.Diagnostic;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

final class ValidEffectCheckMatcher extends TypeSafeMatcher<EffectCheckResult> {
    private ValidEffectCheckMatcher() {}

    @Override
    protected boolean matchesSafely(EffectCheckResult result) {
        return result.isValid();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a valid effect check result");
    }

    @Override
    protected void describeMismatchSafely(EffectCheckResult result, Description description) {
        description.appendText("had diagnostics ").appendValueList("[", ", ", "]",
            result.diagnostics().stream().map(Diagnostic::format).toList());
    }

    static ValidEffectCheckMatcher valid() {
        return new ValidEffectCheckMatcher();
    }
}
