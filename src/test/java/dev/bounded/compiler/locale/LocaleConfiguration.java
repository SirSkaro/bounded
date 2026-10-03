package dev.bounded.compiler.locale;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;

import java.io.IOException;
import java.util.Arrays;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

final class LocaleConfiguration {
    private LocaleConfiguration() {}

    static List<Locale> localesFor(LocaleSensitiveTest annotation) {
        return annotation.value().length == 0 ? defaults() : parse(annotation.value());
    }

    static List<Locale> defaults() {
        try (var input = LocaleConfiguration.class.getResourceAsStream("/locale-sensitive-test.properties")) {
            if (input == null) {
                throw new ExtensionConfigurationException("Missing locale-sensitive-test.properties on the test classpath");
            }
            var properties = new Properties();
            properties.load(input);
            return fromProperties(properties);
        } catch (IOException exception) {
            throw new ExtensionConfigurationException("Cannot read locale-sensitive-test.properties", exception);
        }
    }

    static List<Locale> fromProperties(Properties properties) {
        var locales = properties.getProperty("locales");
        if (locales == null) {
            throw new ExtensionConfigurationException("locale-sensitive-test.properties must define locales");
        }
        return parse(locales.split(",", -1));
    }

    static List<Locale> parse(String... tags) {
        if (tags.length == 0) {
            throw new ExtensionConfigurationException("At least one test locale is required");
        }
        return Arrays.stream(tags).map(LocaleConfiguration::parseLocale).distinct().toList();
    }

    private static Locale parseLocale(String tag) {
        var trimmed = tag.trim();
        if (trimmed.isEmpty()) {
            throw new ExtensionConfigurationException("Test locale must not be blank");
        }
        if (trimmed.equalsIgnoreCase("ROOT")) {
            return Locale.ROOT;
        }
        try {
            return new Locale.Builder().setLanguageTag(trimmed).build();
        } catch (IllformedLocaleException exception) {
            throw new ExtensionConfigurationException("Invalid test locale: " + tag, exception);
        }
    }
}
