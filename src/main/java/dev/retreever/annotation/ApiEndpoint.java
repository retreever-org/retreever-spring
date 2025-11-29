/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.annotation;

import org.springframework.http.HttpStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares additional metadata for an API endpoint.
 * Retreever uses this annotation to enrich endpoint documentation with
 * details such as name, security flags, status codes, headers, and
 * mapped error types.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEndpoint {

    String name() default "";

    boolean secured() default false;

    HttpStatus status() default HttpStatus.OK;

    String[] headers() default {};

    String description() default "";

    Class<? extends Throwable>[] errors() default {};
}
