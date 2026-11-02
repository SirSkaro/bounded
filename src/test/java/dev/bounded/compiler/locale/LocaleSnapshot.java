package dev.bounded.compiler.locale;

import java.util.Locale;

record LocaleSnapshot(Locale defaultLocale, Locale displayLocale, Locale formatLocale) {
    static LocaleSnapshot capture() {
        return new LocaleSnapshot(
                Locale.getDefault(),
                Locale.getDefault(Locale.Category.DISPLAY),
                Locale.getDefault(Locale.Category.FORMAT)
        );
    }

    void restore() {
        Locale.setDefault(defaultLocale);
        Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
        Locale.setDefault(Locale.Category.FORMAT, formatLocale);
    }
}
