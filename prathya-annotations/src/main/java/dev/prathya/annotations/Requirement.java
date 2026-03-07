package dev.prathya.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Links a test method to one or more requirement or corner-case IDs
 * defined in {@code CONTRACT.yaml}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Requirement.List.class)
public @interface Requirement {

    /** One or more requirement or corner-case IDs. */
    String[] value();

    /** Container for repeatable {@link Requirement} annotations. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        Requirement[] value();
    }
}
