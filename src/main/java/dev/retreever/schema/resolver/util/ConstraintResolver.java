/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static dev.retreever.schema.resolver.util.JsonPropertyConstraint.*;

/**
 * Resolves validation constraints from Jakarta Bean Validation annotations.
 * Used during schema generation to attach rules such as nullability,
 * size limits, numeric ranges, and regex patterns to JsonProperty nodes.
 */
public class ConstraintResolver {

    private static final String JAKARTA_PREFIX = "jakarta.validation.constraints.";
    private static final String JAVAX_PREFIX = "javax.validation.constraints.";

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

            switch (a.annotationType().getName()) {

                case JAKARTA_PREFIX + "NotNull", JAVAX_PREFIX + "NotNull" -> result.add(NOT_NULL);
                case JAKARTA_PREFIX + "NotBlank", JAVAX_PREFIX + "NotBlank" -> result.add(NOT_BLANK);
                case JAKARTA_PREFIX + "NotEmpty", JAVAX_PREFIX + "NotEmpty" -> result.add(NOT_EMPTY);

                case JAKARTA_PREFIX + "Size", JAVAX_PREFIX + "Size" -> {
                    int min = intAttribute(a, "min", 0);
                    int max = intAttribute(a, "max", Integer.MAX_VALUE);
                    if (min > 0) result.add(minLength(min));
                    if (max < Integer.MAX_VALUE) result.add(maxLength(max));
                }

                case JAKARTA_PREFIX + "Min", JAVAX_PREFIX + "Min" ->
                        result.add(minValue(numberAttribute(a, "value", 0L).doubleValue()));

                case JAKARTA_PREFIX + "Max", JAVAX_PREFIX + "Max" ->
                        result.add(maxValue(numberAttribute(a, "value", 0L).doubleValue()));

                case JAKARTA_PREFIX + "Pattern", JAVAX_PREFIX + "Pattern" -> {
                    String regexp = stringAttribute(a, "regexp", "");
                    result.add(regex(regexp));
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
        return has(annotations, JAKARTA_PREFIX + "NotNull", JAVAX_PREFIX + "NotNull")
                || has(annotations, JAKARTA_PREFIX + "NotBlank", JAVAX_PREFIX + "NotBlank")
                || has(annotations, JAKARTA_PREFIX + "NotEmpty", JAVAX_PREFIX + "NotEmpty");
    }

    private static boolean has(Annotation[] annotations, String... annotationNames) {
        for (Annotation a : annotations) {
            String annotationName = a.annotationType().getName();
            for (String candidate : annotationNames) {
                if (annotationName.equals(candidate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int intAttribute(Annotation annotation, String attributeName, int defaultValue) {
        Number value = numberAttribute(annotation, attributeName, defaultValue);
        return value.intValue();
    }

    private static Number numberAttribute(Annotation annotation, String attributeName, Number defaultValue) {
        Object value = attributeValue(annotation, attributeName);
        return value instanceof Number number ? number : defaultValue;
    }

    private static String stringAttribute(Annotation annotation, String attributeName, String defaultValue) {
        Object value = attributeValue(annotation, attributeName);
        return value instanceof String string ? string : defaultValue;
    }

    private static Object attributeValue(Annotation annotation, String attributeName) {
        try {
            Method method = annotation.annotationType().getMethod(attributeName);
            return method.invoke(annotation);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
