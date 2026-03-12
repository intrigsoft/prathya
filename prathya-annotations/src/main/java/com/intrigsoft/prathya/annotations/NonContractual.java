package com.intrigsoft.prathya.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a production class or method as intentionally not part of any contract.
 * Annotated elements are excluded from JaCoCo code coverage computation by Prathya,
 * so they don't drag down code coverage metrics (e.g. DTOs, configs, generated code).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NonContractual {

    /** Documents why this code is excluded from contract coverage. */
    String reason();
}
