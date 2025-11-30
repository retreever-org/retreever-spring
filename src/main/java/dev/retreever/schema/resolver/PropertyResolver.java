/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.schema.model.JsonPropertyType;
import dev.retreever.schema.model.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Resolves Field instances into fully enriched Property instances,
 * including constraints, description, and example metadata from annotations.
 */
public class PropertyResolver {

    private static final Logger propLog = LoggerFactory.getLogger(PropertyResolver.class);

    /**
     * Resolves the given Field into a fully enriched {@link Property}.
     * Handles all field types (primitive, object, array) with appropriate metadata.
     *
     * @param field the Field to resolve
     * @return fully enriched Property instance, or null if field is null
     */
    public static Property resolve(Field field) {
        if (field == null) {
            return null;
        }

        Class<?> rawType = field.getType();
        JsonPropertyType propType = JsonPropertyTypeResolver.resolve(rawType);

        Property property = new Property(field.getName(), propType, null);

        propLog.debug("Resolving metadata for prop: {}", property.getName());
        // Enrich with metadata using existing resolvers (safe for all types)
        PropertyConstraintResolver.resolve(property, field);
        PropertyDescriptionResolver.resolve(property, field);
        PropertyExampleResolver.resolve(property, field);

        return property;
    }
}
