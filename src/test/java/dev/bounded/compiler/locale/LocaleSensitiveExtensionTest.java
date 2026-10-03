package dev.bounded.compiler.locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
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

    @DisplayName("Restores distinct default, display, and format locales after every outcome")
    @ParameterizedTest(name = "Restores locales after {0}")
    @ValueSource(classes = {OverriddenLocales.class, BodyFailure.class, SetupFailure.class, TeardownFailure.class})
    void restoresLocales(Class<?> fixture) {
        try (var original = LocaleSnapshot.capture()) {
            Locale.setDefault(Locale.CANADA);
            Locale.setDefault(Locale.Category.DISPLAY, Locale.JAPAN);
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            var expected = LocaleSnapshot.capture();

            var result = run(fixture);

            assertThat(result.summary().getTestsFailedCount(), is(fixture == OverriddenLocales.class ? 0L : 1L));
            assertThat(LocaleSnapshot.capture(), is(expected));
        }
    }

    @Test
    @DisplayName("Rejects locale-sensitive tests without an isolated test container")
    void requiresIsolation() {
        var result = run(UnisolatedFixture.class);

        assertThat(result.summary().getTotalFailureCount(), is(1L));
        assertThat(result.summary().getFailures().getFirst().getException().getMessage(),
                containsString("requires @Isolated"));
    }

    @Test
    @DisplayName("Accepts isolation declared on an enclosing test container")
    void supportsNestedTests() {
        var result = run(IsolatedContainer.class);

        assertThat(result.summary().getTotalFailureCount(), is(0L));
        assertThat(result.names(), contains("ROOT"));
    }

    @Test
    @DisplayName("Rejects malformed annotation locale overrides")
    void rejectsMalformedOverride() {
        var result = run(MalformedLocale.class);

        assertThat(result.summary().getTotalFailureCount(), is(1L));
        assertThat(result.summary().getFailures().getFirst().getException().getMessage(),
                containsString("Invalid test locale"));
    }

    @Test
    @DisplayName("Ordinary test runs skip fixtures with deliberate failures")
    void skipsFixturesOutsideTheDedicatedLauncher() {
        var result = run(BodyFailure.class, false);

        assertThat(result.summary().getTotalFailureCount(), is(0L));
        assertThat(result.summary().getContainersSkippedCount(), is(greaterThan(0L)));
        assertThat(result.summary().getTestsStartedCount(), is(0L));
    }

    private RunResult run(Class<?> fixture) {
        return run(fixture, true);
    }

    private RunResult run(Class<?> fixture, boolean enableFixtures) {
        var summary = new SummaryGeneratingListener();
        var names = new ArrayList<String>();
        var request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(fixture))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent");
        if (enableFixtures) {
            request.configurationParameter("junit.jupiter.conditions.deactivate",
                    "org.junit.jupiter.engine.extension.DisabledCondition");
        }
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
    @ExtensionTestFixture
    static class ConfiguredLocales {
        @LocaleSensitiveTest
        void observesConfiguredLocale() {
            assertThat(LocaleConfiguration.defaults().contains(Locale.getDefault()), is(true));
        }
    }

    @Isolated
    @ExtensionTestFixture
    static class OverriddenLocales {
        @LocaleSensitiveTest("ja-JP")
        void observesOverride() {
            assertThat(Locale.getDefault(), is(Locale.JAPAN));
        }
    }

    @Isolated
    @ExtensionTestFixture
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
    @ExtensionTestFixture
    static class BodyFailure {
        @LocaleSensitiveTest("tr-TR")
        void fails() {
            throw new AssertionError("Deliberate test failure");
        }
    }

    @Isolated
    @ExtensionTestFixture
    static class SetupFailure {
        @BeforeEach
        void fails() {
            throw new IllegalStateException("Deliberate setup failure");
        }

        @LocaleSensitiveTest("tr-TR")
        void body() {}
    }

    @Isolated
    @ExtensionTestFixture
    static class TeardownFailure {
        @LocaleSensitiveTest("tr-TR")
        void body() {}

        @AfterEach
        void fails() {
            throw new IllegalStateException("Deliberate teardown failure");
        }
    }

    @ExtensionTestFixture
    static class UnisolatedFixture {
        @LocaleSensitiveTest("tr-TR")
        void body() {}
    }

    @Isolated
    @ExtensionTestFixture
    static class IsolatedContainer {
        @Nested
        class NestedTests {
            @LocaleSensitiveTest("ROOT")
            void observesRootLocale() {
                assertThat(Locale.getDefault(), is(Locale.ROOT));
            }
        }
    }

    @Isolated
    @ExtensionTestFixture
    static class MalformedLocale {
        @LocaleSensitiveTest("tr_TR")
        void body() {}
    }
}
