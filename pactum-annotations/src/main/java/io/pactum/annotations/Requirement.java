package io.pactum.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Links a pact or test to one or more requirement identifiers.
 * Use this annotation to trace verification coverage back to specific requirements.
 */
@Documented
@Repeatable(Requirements.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Requirement {

    /**
     * The unique identifier of the requirement (e.g. a JIRA ticket, story ID, or spec section).
     */
    String id();

    /**
     * A brief description of the requirement.
     */
    String description() default "";
}
