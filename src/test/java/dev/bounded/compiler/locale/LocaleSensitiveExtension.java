package dev.bounded.compiler.locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.stream.Stream;

public final class LocaleSensitiveExtension implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return AnnotationSupport.isAnnotated(context.getRequiredTestMethod(), LocaleSensitiveTest.class);
    }

    @Override
    public Stream<? extends TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        requireIsolation(context);
        var method = context.getRequiredTestMethod();
        if (AnnotationSupport.isAnnotated(method, Test.class)
                || AnnotationSupport.isAnnotated(method, ParameterizedTest.class)) {
            throw new ExtensionConfigurationException("Use @LocaleSensitiveTest instead of @Test or @ParameterizedTest");
        }
        var annotation = AnnotationSupport.findAnnotation(method, LocaleSensitiveTest.class).orElseThrow();
        return LocaleConfiguration.localesFor(annotation).stream().map(LocaleInvocation::new);
    }

    private void requireIsolation(ExtensionContext context) {
        for (var current = context; current != null; current = current.getParent().orElse(null)) {
            if (current.getTestClass().filter(type -> AnnotationSupport.isAnnotated(type, Isolated.class)).isPresent()) {
                return;
            }
        }
        throw new ExtensionConfigurationException("@LocaleSensitiveTest requires @Isolated on the test class or an enclosing test container");
    }
}
