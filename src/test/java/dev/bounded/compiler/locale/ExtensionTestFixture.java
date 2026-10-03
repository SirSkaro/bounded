package dev.bounded.compiler.locale;

import org.junit.jupiter.api.Disabled;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Disabled("Executed only by LocaleSensitiveExtensionTest's launcher")
@interface ExtensionTestFixture {}
