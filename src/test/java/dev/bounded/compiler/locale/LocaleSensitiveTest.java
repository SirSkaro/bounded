package dev.bounded.compiler.locale;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(LocaleSensitiveExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
public @interface LocaleSensitiveTest {
    String[] value() default {};
}
