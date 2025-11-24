/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.domain.model.JsonPropertyType;

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
            return JsonPropertyType.NUMBER; // byte, short, int, long, float, double
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

        // Collections like List, Set, Queue...
        if (Collection.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.ARRAY;
        }

        // Enums → Enum type
        if (clazz.isEnum()) {
            return JsonPropertyType.ENUM;
        }

        // Known special types
        if (clazz == UUID.class) {
            return JsonPropertyType.UUID;
        }

        // Temporal types (mapped to RFC-3339 compliant types)
        if (clazz == Instant.class) return JsonPropertyType.DATE_TIME;
        if (clazz == LocalDate.class) return JsonPropertyType.DATE;
        if (clazz == LocalTime.class) return JsonPropertyType.TIME;
        if (clazz == LocalDateTime.class) return JsonPropertyType.DATE_TIME;
        if (clazz == OffsetDateTime.class) return JsonPropertyType.DATE_TIME;
        if (clazz == ZonedDateTime.class) return JsonPropertyType.DATE_TIME;

        // Duration / Period → semantically useful for clients
        if (clazz == Duration.class) return JsonPropertyType.DURATION;
        if (clazz == Period.class) return JsonPropertyType.PERIOD;

        // URI / URL
        if (clazz == URI.class || clazz == URL.class) {
            return JsonPropertyType.URI;
        }

        // InputStreams represent binary payloads
        if (InputStream.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.BINARY;
        }

        // Maps → JSON Object
        if (Map.class.isAssignableFrom(clazz)) {
            return JsonPropertyType.OBJECT;
        }

        // Void wrapper
        if (clazz == Void.class) {
            return JsonPropertyType.NULL;
        }

        // Anything else → treat as complex object
        return JsonPropertyType.OBJECT;
    }
}
