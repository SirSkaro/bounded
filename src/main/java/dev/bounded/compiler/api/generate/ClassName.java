package dev.bounded.compiler.api.generate;

import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public record ClassName(String binaryName) {
    private static final String IDENTIFIER = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
    private static final Pattern BINARY_NAME = Pattern.compile(IDENTIFIER + "(?:\\." + IDENTIFIER + ")*");

    public ClassName {
        requireNonNull(binaryName, "binaryName");
        if (!BINARY_NAME.matcher(binaryName).matches()
                || binaryName.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid binary class name: " + binaryName);
        }
    }

    public String resourceName() {
        return binaryName.replace('.', '/') + ".class";
    }
}
