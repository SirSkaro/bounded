package dev.bounded.compiler.acceptance;

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.allOf;

public final class CompilationMatchers {
    private CompilationMatchers() {}

    public static Matcher<CompilationOutcome> accepted() {
        return new CompilationStatusMatcher(true);
    }

    public static Matcher<CompilationOutcome> rejected() {
        return new CompilationStatusMatcher(false);
    }

    public static Matcher<CompilationOutcome> rejectedWithCode(String code) {
        return allOf(rejected(), new DiagnosticCodeMatcher(code));
    }
}
