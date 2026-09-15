package dev.bounded.compiler.grammar;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

final class OperationMatcher extends TypeSafeMatcher<OperationFacade> {
    private final String expectedKind;
    private final String expectedName;

    OperationMatcher(String expectedKind, String expectedName) {
        this.expectedKind = expectedKind;
        this.expectedName = expectedName;
    }

    @Override
    protected boolean matchesSafely(OperationFacade operation) {
        return expectedKind.equals(operation.kind()) && expectedName.equals(operation.name());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("an operation with kind ").appendValue(expectedKind)
                .appendText(" and name ").appendValue(expectedName);
    }

    @Override
    protected void describeMismatchSafely(OperationFacade operation, Description description) {
        description.appendText("had kind ").appendValue(operation.kind())
                .appendText(" and name ").appendValue(operation.name());
    }
}
