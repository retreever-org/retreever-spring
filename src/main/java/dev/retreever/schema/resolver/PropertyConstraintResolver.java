/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.schema.model.Property;
import dev.retreever.schema.resolver.util.ConstraintResolver;
import dev.retreever.schema.resolver.util.JsonPropertyConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * Applies validation-based constraints to a {@link Property}.
 * Delegates extraction logic to {@link ConstraintResolver} and
 * marks properties as required when applicable.
 */
public class PropertyConstraintResolver {

    private static final Logger constraintLog = LoggerFactory.getLogger(PropertyConstraintResolver.class);

    /**
     * Populates constraint markers on the given JsonProperty
     * based on the validation annotations declared on the field.
     *
     * @param prop the property being enriched
     * @param field    the source field inspected via reflection
     */
    public static void resolve(Property prop, Field field) {
        constraintLog.debug("Resolving Constraint for prop: {}", prop.getName());
        Annotation[] anns = field.getAnnotations();

        // Normal constraints
        Set<String> constraints = ConstraintResolver.resolve(anns);
        constraints.forEach(prop::addConstraint);

        // ENUM handling
        Class<?> type = field.getType();
        if (type.isEnum()) {
            appendAllowedValueConstraintIfEnum(prop, type);
        }

        if (ConstraintResolver.isRequired(anns)) {
            prop.required();
        }

        constraintLog.debug("Property: {}, Constraints: {}", prop.getName(), constraints);
    }

    private static void appendAllowedValueConstraintIfEnum(Property prop, Class<?> type) {
        Object[] constants = type.getEnumConstants();
        String[] names = new String[constants.length];

        for (int i = 0; i < constants.length; i++) {
            names[i] = ((Enum<?>) constants[i]).name();
        }

        String enumConstraint = JsonPropertyConstraint.enumValue(names);
        prop.addConstraint(enumConstraint);
    }
}
