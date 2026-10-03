package dev.bounded.compiler.locale;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.util.List;
import java.util.Locale;

record LocaleInvocation(Locale locale) implements TestTemplateInvocationContext {
    @Override
    public String getDisplayName(int invocationIndex) {
        return locale.equals(Locale.ROOT) ? "ROOT" : locale.toLanguageTag();
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return List.of(new LocaleLifecycle(locale));
    }
}
