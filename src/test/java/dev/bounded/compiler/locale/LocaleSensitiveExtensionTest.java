package dev.bounded.compiler.locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@Isolated
class LocaleSensitiveExtensionTest {
    @Test
    @DisplayName("Runs once per configured locale with readable invocation names")
    void usesConfiguredLocales() {
        var result = run(ConfiguredLocales.class);

        assertThat(result.summary().getTotalFailureCount(), is(0L));
        var expectedNames = LocaleConfiguration.defaults().stream()
                .map(locale -> locale.equals(Locale.ROOT) ? "ROOT" : locale.toLanguageTag())
                .toList();
        assertThat(result.names(), is(expectedNames));
    }

    @Test
    @DisplayName("Annotation locales replace the global configuration")
    void overridesConfiguredLocales() {
        var result = run(OverriddenLocales.class);

        assertThat(result.summary().getTotalFailureCount(), is(0L));
        assertThat(result.names(), contains("ja-JP"));
    }

    @Test
    @DisplayName("The selected locale applies to setup, the test, and teardown")
    void appliesLocaleThroughoutTheLifecycle() {
        LifecycleFixture.observed.clear();

        var result = run(LifecycleFixture.class);

        assertThat(result.summary().getTotalFailureCount(), is(0L));
        var locale = Locale.forLanguageTag("tr-TR");
        var expected = new LocaleSnapshot(locale, locale, locale);
        assertThat(LifecycleFixture.observed, contains(expected, expected, expected));
    }

    @Test
    @DisplayName("Restores distinct default, display, and format locales after a successful test")
    void restoresLocales() {
        try (var original = LocaleSnapshot.capture()) {
            Locale.setDefault(Locale.CANADA);
            Locale.setDefault(Locale.Category.DISPLAY, Locale.JAPAN);
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            var expected = LocaleSnapshot.capture();

            var result = run(OverriddenLocales.class);

            assertThat(result.summary().getTotalFailureCount(), is(0L));
            assertThat(LocaleSnapshot.capture(), is(expected));
        }
    }

    @Test
    @DisplayName("Accepts isolation declared on an enclosing test container")
    void supportsNestedTests() {
        var result = run(IsolatedContainer.class);

        assertThat(result.summary().getTotalFailureCount(), is(0L));
        assertThat(result.names(), contains("ROOT"));
    }

    private RunResult run(Class<?> fixture) {
        var summary = new SummaryGeneratingListener();
        var names = new ArrayList<String>();
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(fixture))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent");
        var listener = new TestExecutionListener() {
            @Override
            public void executionStarted(TestIdentifier identifier) {
                if (identifier.isTest()) {
                    names.add(identifier.getDisplayName());
                }
            }
        };
        try (var session = LauncherFactory.openSession()) {
            session.getLauncher().execute(request.build(), summary, listener);
        }
        return new RunResult(summary.getSummary(), List.copyOf(names));
    }

    private record RunResult(TestExecutionSummary summary, List<String> names) {}

    @Isolated
    static class ConfiguredLocales {
        @LocaleSensitiveTest
        void observesConfiguredLocale() {
            assertThat(LocaleConfiguration.defaults().contains(Locale.getDefault()), is(true));
        }
    }

    @Isolated
    static class OverriddenLocales {
        @LocaleSensitiveTest("ja-JP")
        void observesOverride() {
            assertThat(Locale.getDefault(), is(Locale.JAPAN));
        }
    }

    @Isolated
    static class LifecycleFixture {
        static final List<LocaleSnapshot> observed = new ArrayList<>();

        @BeforeEach
        void before() {
            observed.add(LocaleSnapshot.capture());
        }

        @LocaleSensitiveTest("tr-TR")
        void body() {
            observed.add(LocaleSnapshot.capture());
        }

        @AfterEach
        void after() {
            observed.add(LocaleSnapshot.capture());
        }
    }

    @Isolated
    static class IsolatedContainer {
        @Nested
        class NestedTests {
            @LocaleSensitiveTest("ROOT")
            void observesRootLocale() {
                assertThat(Locale.getDefault(), is(Locale.ROOT));
            }
        }
    }

}
