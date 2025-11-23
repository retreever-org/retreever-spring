/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.schema.resolver.util;

import jakarta.validation.constraints.*;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public class ConstraintResolver {

    /**
     * Resolves bean validation constraints from annotations.
     */
    public static Set<String> resolve(Annotation[] annotations) {

        Set<String> result = new HashSet<>();

        for (Annotation a : annotations) {

            switch (a.annotationType().getSimpleName()) {

                case "NotNull" -> result.add("NOT_NULL");
                case "NotBlank" -> result.add("NOT_BLANK");
                case "NotEmpty" -> result.add("NOT_EMPTY");

                case "Size" -> {
                    Size s = (Size) a;
                    if (s.min() > 0) result.add("MIN_LENGTH:" + s.min());
                    if (s.max() < Integer.MAX_VALUE) result.add("MAX_LENGTH:" + s.max());
                }

                case "Min" -> {
                    Min m = (Min) a;
                    result.add("MIN_VALUE:" + m.value());
                }

                case "Max" -> {
                    Max m = (Max) a;
                    result.add("MAX_VALUE:" + m.value());
                }

                case "Pattern" -> {
                    Pattern p = (Pattern) a;
                    result.add("PATTERN:" + p.regexp());
                }
            }
        }

        return result;
    }

    /**
     * Determines if the constraints imply `required=true`.
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

