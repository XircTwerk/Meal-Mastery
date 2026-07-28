package com.xirc.mealmastery.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explanatory text written above a key or section in the config file.
 *
 * <p>Javadoc cannot do this job: it is stripped at compile time, so the only
 * way for the generated file to explain itself is to carry the text into the
 * class file. Each element is one line, already wrapped — the writer adds the
 * {@code #} and the indentation, nothing else.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Comment {
    String[] value();
}
