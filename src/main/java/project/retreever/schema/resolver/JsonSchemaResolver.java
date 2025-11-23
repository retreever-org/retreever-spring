/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.schema.resolver;

import project.retreever.domain.model.JsonProperty;
import project.retreever.domain.model.JsonPropertyType;

import java.lang.reflect.*;
import java.util.*;

/**
 * Resolves JSON-schema-like structures for Java classes.
 * Performs recursive inspection of fields, detects arrays/collections,
 * resolves nested objects, and applies metadata (constraints,
 * descriptions, examples) via specialized resolvers.
 *
 * <p>Handles:</p>
 * <ul>
 *     <li>Primitive, enum, object, and array types</li>
 *     <li>Generic collection element extraction</li>
 *     <li>Cyclic reference detection</li>
 *     <li>Annotation-driven metadata enrichment</li>
 * </ul>
 */
public class JsonSchemaResolver {

    /**
     * Tracks types currently being processed to avoid infinite loops
     * when encountering self-referential or cyclic class structures.
     */
    private final Set<Class<?>> visited = new HashSet<>();

    /**
     * Resolves all JSON schema properties for the given class.
     *
     * @param clazz the Java class to scan
     * @return a list of top-level JsonProperty entries
     */
    public List<JsonProperty> resolve(Class<?> clazz) {
        if (clazz == null || visited.contains(clazz)) {
            return List.of();
        }

        visited.add(clazz);

        List<JsonProperty> schema = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            schema.add(resolveField(field));
        }

        visited.remove(clazz);
        return schema;
    }

    /**
     * Converts a single field into its corresponding JsonProperty tree.
     *
     * @param field the field to inspect
     * @return resolved JsonProperty
     */
    private JsonProperty resolveField(Field field) {
        Type type = field.getGenericType();
        Class<?> rawClass = TypeResolver.extractRawClass(type);

        JsonPropertyType propType = JsonPropertyTypeResolver.resolve(rawClass);
        JsonProperty jsonProp = JsonProperty.of(field.getName(), propType);

        switch (propType) {
            case OBJECT -> handleObject(jsonProp, rawClass);
            case ARRAY -> handleArray(jsonProp, type);
            default -> handlePrimitive(jsonProp, field);
        }

        return jsonProp;
    }

    /**
     * Resolves nested object structure recursively.
     *
     * @param jsonProp parent JSON property
     * @param rawClass class representing the nested object
     */
    private void handleObject(JsonProperty jsonProp, Class<?> rawClass) {
        List<JsonProperty> nested = resolve(rawClass);
        nested.forEach(jsonProp::addProperty);
    }

    /**
     * Resolves array or collection element schemas.
     *
     * @param jsonProp representing the array property
     * @param fieldType the raw or generic type for the array/collection
     */
    private void handleArray(JsonProperty jsonProp, Type fieldType) {
        Type elemType = extractElementType(fieldType);
        if (elemType == null) return;

        Class<?> elemClass = TypeResolver.extractRawClass(elemType);
        List<JsonProperty> elementSchema = resolve(elemClass);

        if (!elementSchema.isEmpty()) {
            // Use first property as schema reference for elements
            jsonProp.arrayElement(elementSchema.get(0));
        }
    }

    /**
     * Applies annotation-driven metadata for primitive-like types.
     *
     * @param jsonProp the target property
     * @param field    source field with annotations
     */
    private void handlePrimitive(JsonProperty jsonProp, Field field) {
        JsonPropertyConstraintResolver.resolve(jsonProp, field);
        JsonPropertyDescriptionResolver.resolve(jsonProp, field);
        JsonPropertyExampleResolver.resolve(jsonProp, field);
    }

    /**
     * Extracts an element type from an array or generic collection.
     *
     * @param type raw or generic type
     * @return detected element type or null
     */
    private Type extractElementType(Type type) {
        if (type instanceof Class<?> clazz && clazz.isArray()) {
            return clazz.getComponentType();
        }
        return TypeResolver.getTypeParameter(type);
    }
}
