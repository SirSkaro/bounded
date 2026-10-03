package dev.bounded.compiler.locale;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Locale;

final class LocaleLifecycle implements BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(LocaleLifecycle.class);
    private final Locale locale;

    LocaleLifecycle(Locale locale) {
        this.locale = locale;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getStore(NAMESPACE).put(LocaleSnapshot.class, LocaleSnapshot.capture());
        Locale.setDefault(locale);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var snapshot = context.getStore(NAMESPACE).remove(LocaleSnapshot.class, LocaleSnapshot.class);
        if (snapshot != null) {
            snapshot.close();
        }
    }
}
