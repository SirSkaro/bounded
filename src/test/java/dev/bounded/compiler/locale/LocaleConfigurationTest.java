package dev.bounded.compiler.locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocaleConfigurationTest {
    @Test
    @DisplayName("Parses comma-separated locale tags and the ROOT alias")
    void parsesConfiguredLocales() {
        var properties = new Properties();
        properties.setProperty("locales", " ROOT, tr-TR, ar-EG, de-DE ");

        assertThat(LocaleConfiguration.fromProperties(properties), contains(
                Locale.ROOT,
                Locale.forLanguageTag("tr-TR"),
                Locale.forLanguageTag("ar-EG"),
                Locale.GERMANY
        ));
    }

    @Test
    @DisplayName("Runs duplicate locales only once while preserving configured order")
    void removesDuplicates() {
        assertThat(LocaleConfiguration.parse("tr-TR", "ROOT", "tr-tr", "root"),
                contains(Locale.forLanguageTag("tr-TR"), Locale.ROOT));
    }

    @Test
    @DisplayName("Requires the locales property")
    void rejectsMissingConfiguration() {
        assertThrows(ExtensionConfigurationException.class,
                () -> LocaleConfiguration.fromProperties(new Properties()));
    }

    @DisplayName("Rejects blank entries and malformed locale tags")
    @ParameterizedTest(name = "Rejects locale configuration [{0}]")
    @ValueSource(strings = {"", " ", "ROOT,", "tr_TR", "en--US"})
    void rejectsInvalidConfiguration(String value) {
        var properties = new Properties();
        properties.setProperty("locales", value);

        assertThrows(ExtensionConfigurationException.class,
                () -> LocaleConfiguration.fromProperties(properties));
    }
}
