/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.schema.model.JsonPropertyType;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.*;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Infers the most suitable {@link JsonPropertyType} for a Java class.
 * <p>
 * This resolver improves documentation quality by mapping special Java types
 * (UUID, temporal types, binary types, URI/URL, enums, collections, etc.)
 * to more precise schema categories instead of treating everything as
 * a generic string or object.
 */
public class JsonPropertyTypeResolver {

    /**
     * Determines the JSON-like schema type for the given Java class.
     *
     * @param clazz the Java type to inspect
     * @return inferred {@link JsonPropertyType}
     */
    public static JsonPropertyType resolve(Class<?> clazz) {

        if (clazz == null) {
            return JsonPropertyType.OBJECT;
        }

        // Primitive types
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class) return JsonPropertyType.BOOLEAN;
            if (clazz == char.class) return JsonPropertyType.STRING;
            if (clazz == void.class) return JsonPropertyType.NULL;
            return JsonPropertyType.NUMBER;
        }

        // Wrapper types
        if (Number.class.isAssignableFrom(clazz)) return JsonPropertyType.NUMBER;
        if (Boolean.class.isAssignableFrom(clazz)) return JsonPropertyType.BOOLEAN;
        if (CharSequence.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz))
            return JsonPropertyType.STRING;

        // Arrays (& byte[] handled specially)
        if (clazz.isArray()) {
            if (clazz == byte[].class || clazz == Byte[].class) {
                return JsonPropertyType.BINARY;
            }
            return JsonPropertyType.ARRAY;
        }

        // Collections like List, Set...
        if (Collection.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.ARRAY;
        }

        // Maps → treat as ARRAY (value schema)
        if (Map.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.MAP;
        }

        // Enums
        if (clazz.isEnum()) {
            return JsonPropertyType.ENUM;
        }

        // Special types
        if (clazz == UUID.class) return JsonPropertyType.UUID;

        if (clazz == Instant.class) return JsonPropertyType.DATE_TIME;
        if (clazz == LocalDate.class) return JsonPropertyType.DATE;
        if (clazz == LocalTime.class) return JsonPropertyType.TIME;
        if (clazz == LocalDateTime.class) return JsonPropertyType.DATE_TIME;
        if (clazz == OffsetDateTime.class) return JsonPropertyType.DATE_TIME;
        if (clazz == ZonedDateTime.class) return JsonPropertyType.DATE_TIME;

        if (clazz == Duration.class) return JsonPropertyType.DURATION;
        if (clazz == Period.class) return JsonPropertyType.PERIOD;

        if (clazz == URI.class || clazz == URL.class) {
            return JsonPropertyType.URI;
        }

        if (InputStream.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.BINARY;
        }

        if (clazz == Void.class) {
            return JsonPropertyType.NULL;
        }

        // Records & POJOs
        return JsonPropertyType.OBJECT;
    }

}
