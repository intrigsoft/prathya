package io.pactum.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method as a pact (contract) between a consumer and a provider.
 * Classes or methods annotated with {@code @Pact} describe an expected interaction
 * that pactum will verify at build time.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Pact {

    /**
     * The name of the consumer that owns this pact.
     */
    String consumer() default "";

    /**
     * The name of the provider that this pact is verified against.
     */
    String provider() default "";

    /**
     * A human-readable description of this pact.
     */
    String description() default "";
}
