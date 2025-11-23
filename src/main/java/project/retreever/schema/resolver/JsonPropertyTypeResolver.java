/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.schema.resolver;

import project.retreever.domain.model.JsonPropertyType;

import java.util.Collection;
import java.util.Map;

/**
 * Determines the appropriate {@link JsonPropertyType} for a given Java class.
 * Handles primitives, wrappers, collections, arrays, enums, maps, and defaults
 * to OBJECT for custom types.
 */
public class JsonPropertyTypeResolver {

    /**
     * Resolves the JSON schema type corresponding to the provided class.
     *
     * @param clazz the Java type to inspect
     * @return the inferred JsonPropertyType
     */
    public static JsonPropertyType resolve(Class<?> clazz) {

        if (clazz == null) {
            return JsonPropertyType.OBJECT;
        }

        // Handle primitives and wrappers for Number
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class) {
                return JsonPropertyType.BOOLEAN;
            } else if (clazz == char.class) {
                return JsonPropertyType.STRING;
            } else if (clazz == void.class) {
                return JsonPropertyType.NULL;
            }
            // Numeric primitives
            else {
                return JsonPropertyType.NUMBER;
            }
        }

        // Handle wrappers and common classes
        if (Number.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.NUMBER;
        }
        if (Boolean.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.BOOLEAN;
        }
        if (CharSequence.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.STRING;
        }
        if (clazz.isArray() || Collection.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.ARRAY;
        }
        if (clazz.isEnum()) {
            return JsonPropertyType.ENUM;
        }
        if (Map.class.isAssignableFrom(clazz)) {
            // Maps can be modeled as OBJECT for JSON object equivalent
            return JsonPropertyType.OBJECT;
        }
        if (clazz == Void.class) {
            return JsonPropertyType.NULL;
        }

        // Default fallback for custom or other objects
        return JsonPropertyType.OBJECT;
    }
}
