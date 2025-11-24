/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver.util;

import jakarta.validation.constraints.*;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import static dev.retreever.schema.resolver.util.JsonPropertyConstraint.*;

/**
 * Resolves validation constraints from Jakarta Bean Validation annotations.
 * Used during schema generation to attach rules such as nullability,
 * size limits, numeric ranges, and regex patterns to JsonProperty nodes.
 */
public class ConstraintResolver {

    /**
     * Extracts constraint descriptors from the given annotations.
     * Only a subset of commonly used validation annotations is supported.
     *
     * @param annotations the annotations declared on a field or parameter
     * @return a set of formatted constraint strings
     */
    public static Set<String> resolve(Annotation[] annotations) {

        Set<String> result = new HashSet<>();

        for (Annotation a : annotations) {

            switch (a.annotationType().getSimpleName()) {

                case "NotNull" -> result.add(NOT_NULL);
                case "NotBlank" -> result.add(NOT_BLANK);
                case "NotEmpty" -> result.add(NOT_EMPTY);

                case "Size" -> {
                    Size s = (Size) a;
                    if (s.min() > 0) result.add(minLength(s.min()));
                    if (s.max() < Integer.MAX_VALUE) result.add(maxLength(s.max()));
                }

                case "Min" -> {
                    Min m = (Min) a;
                    result.add(minValue(m.value()));
                }

                case "Max" -> {
                    Max m = (Max) a;
                    result.add(maxValue(m.value()));
                }

                case "Pattern" -> {
                    Pattern p = (Pattern) a;
                    result.add(regex( p.regexp()));
                }
            }
        }

        return result;
    }

    /**
     * Determines whether the provided annotations imply the value must be present.
     *
     * @param annotations the annotations to inspect
     * @return true if a NotNull, NotBlank, or NotEmpty annotation is present
     */
    public static boolean isRequired(Annotation[] annotations) {
        return has(annotations, NotNull.class)
                || has(annotations, NotBlank.class)
                || has(annotations, NotEmpty.class);
    }

    private static boolean has(Annotation[] annotations, Class<? extends Annotation> aClass) {
        for (Annotation a : annotations) {
            if (a.annotationType() == aClass) return true;
        }
        return false;
    }
}
