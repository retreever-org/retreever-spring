/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.domain.model.JsonProperty;
import dev.retreever.domain.model.JsonPropertyType;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves JSON-schema-like structures for Java classes.
 * Expands only domain model classes belonging to the application's base package.
 * Prevents infinite recursion using a visited set, returning lightweight reference
 * nodes when encountering self-referential structures.
 */
public class JsonSchemaResolver {

    private final Set<Class<?>> visited = new HashSet<>();
    private final List<String> basePackages;

    /**
     * @param basePackages the consumer application's root package,
     *                    used to decide which classes qualify as domain models.
     */
    public JsonSchemaResolver(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    /**
     * Resolves the schema for a Java class into a list of JsonProperty entries.
     * Returns a reference node if recursion is detected.
     */
    public List<JsonProperty> resolve(Class<?> clazz) {
        if (clazz == null) {
            return List.of();
        }

        if (visited.contains(clazz)) {
            return List.of(JsonProperty.reference(clazz.getSimpleName()));
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
     * Converts a Java field into its JsonProperty representation.
     */
    private JsonProperty resolveField(Field field) {
        Type type = field.getGenericType();
        Class<?> rawClass = TypeResolver.extractRawClass(type);

        JsonPropertyType propType = JsonPropertyTypeResolver.resolve(rawClass);
        JsonProperty jsonProp = JsonProperty.of(field.getName(), propType);

        switch (propType) {
            case OBJECT -> handleObject(jsonProp, rawClass);
            case ARRAY -> handleArray(jsonProp, field.getGenericType());
            default -> handlePrimitive(jsonProp, field);
        }

        return jsonProp;
    }

    /**
     * Expands nested object properties only when the type belongs to the consumer's domain package.
     */
    private void handleObject(JsonProperty jsonProp, Class<?> rawClass) {

        if (rawClass == null) {
            return;
        }

        JsonPropertyType type = JsonPropertyTypeResolver.resolve(rawClass);
        if (type != JsonPropertyType.OBJECT) {
            return;
        }

        if (!isDomainModel(rawClass)) {
            return;
        }

        if (visited.contains(rawClass)) {
            jsonProp.addProperty(JsonProperty.reference(rawClass.getSimpleName()));
            return;
        }

        visited.add(rawClass);
        List<JsonProperty> nested = resolve(rawClass);
        visited.remove(rawClass);

        nested.forEach(jsonProp::addProperty);
    }


    /**
     * Determines whether a class belongs to the consumer application's domain model.
     */
    private boolean isDomainModel(Class<?> clazz) {
        if (clazz == null) return false;

        Package pkg = clazz.getPackage();
        if (pkg == null) return false;

        String name = pkg.getName();
        if (name.isEmpty()) return false;

        for (String base : basePackages) {
            if (name.startsWith(base)) {
                return true;
            }
        }

        return false;
    }



    /**
     * Resolves array/collection element schema safely with recursion handling.
     */
    private void handleArray(JsonProperty jsonProp, Type fieldType) {
        Type elemType = extractElementType(fieldType);
        if (elemType == null) {
            return;
        }

        Class<?> elemClass = TypeResolver.extractRawClass(elemType);

        if (elemClass != null && visited.contains(elemClass)) {
            jsonProp.arrayElement(JsonProperty.reference(elemClass.getSimpleName()));
            return;
        }

        List<JsonProperty> elementProps = resolve(elemClass);
        if (!elementProps.isEmpty()) {
            jsonProp.arrayElement(elementProps.get(0));
        }
    }

    /**
     * Applies metadata from annotations for primitive-like types.
     */
    private void handlePrimitive(JsonProperty jsonProp, Field field) {
        JsonPropertyConstraintResolver.resolve(jsonProp, field);
        JsonPropertyDescriptionResolver.resolve(jsonProp, field);
        JsonPropertyExampleResolver.resolve(jsonProp, field);
    }

    /**
     * Extracts element type for arrays or generic collections.
     */
    private Type extractElementType(Type type) {
        if (type instanceof Class<?> clazz && clazz.isArray()) {
            return clazz.getComponentType();
        }
        return TypeResolver.getTypeParameter(type);
    }
}
