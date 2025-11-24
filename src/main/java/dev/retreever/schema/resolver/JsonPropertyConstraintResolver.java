/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.domain.model.JsonProperty;
import dev.retreever.schema.resolver.util.ConstraintResolver;
import dev.retreever.schema.resolver.util.JsonPropertyConstraint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * Applies validation-based constraints to a {@link JsonProperty}.
 * Delegates extraction logic to {@link ConstraintResolver} and
 * marks properties as required when applicable.
 */
public class JsonPropertyConstraintResolver {

    /**
     * Populates constraint markers on the given JsonProperty
     * based on the validation annotations declared on the field.
     *
     * @param jsonProp the property being enriched
     * @param field    the source field inspected via reflection
     */
    public static void resolve(JsonProperty jsonProp, Field field) {
        Annotation[] anns = field.getAnnotations();

        // Normal constraints
        Set<String> constraints = ConstraintResolver.resolve(anns);
        constraints.forEach(jsonProp::addConstraint);

        // ENUM handling
        Class<?> type = field.getType();
        if (type.isEnum()) {
            appendAllowedValueConstraintIfEnum(jsonProp, type);
        }

        if (ConstraintResolver.isRequired(anns)) {
            jsonProp.required();
        }
    }

    private static void appendAllowedValueConstraintIfEnum(JsonProperty jsonProp, Class<?> type) {
            Object[] constants = type.getEnumConstants();
            String[] names = new String[constants.length];

            for (int i = 0; i < constants.length; i++) {
                names[i] = ((Enum<?>) constants[i]).name();
            }

            String enumConstraint = JsonPropertyConstraint.enumValue(names);
            jsonProp.addConstraint(enumConstraint);
    }
}
