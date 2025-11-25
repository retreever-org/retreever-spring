/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.config.SchemaConfig;
import dev.retreever.domain.model.JsonProperty;

import java.lang.reflect.*;
import java.util.*;

/**
 * Resolves the schema of a Java object type into a list of {@link JsonProperty}.
 *
 * <p>This resolver handles only <b>direct self-recursion</b>:
 * <ul>
 *   <li>A type referencing itself directly (A → A)</li>
 *   <li>A type referencing a collection/array/map of itself (A → List&lt;A&gt;)</li>
 * </ul>
 *
 * <p>When such recursion is detected, a {@code JsonProperty.reference(...)} is returned
 * instead of expanding the object again, preventing infinite loops.</p>
 *
 * <p>Deep or mutual recursion (A → B → A) is <b>not</b> handled here by design,
 * as requested. Only the immediate self-recursion cases are guarded.</p>
 *
 * <p>The resolver performs:</p>
 * <ul>
 *   <li>Generic type substitution through {@link GenericContext}</li>
 *   <li>Domain model filtering based on {@link SchemaConfig}</li>
 *   <li>Field-by-field delegation to {@link JsonPropertyResolver}</li>
 * </ul>
 */
public class JsonObjectResolver {

    private final GenericContext context;

    public JsonObjectResolver(GenericContext context) {
        this.context = context;
    }

    /**
     * Resolves the given {@link Type} into a list of JSON schema properties.
     * Performs generic unfolding, domain filtering, and direct self-recursion checks.
     *
     * @param type the type to resolve
     * @return list of {@link JsonProperty}, or a list containing a reference node if recursion is detected
     */
    public List<JsonProperty> resolve(Type type) {

        if (type == null) {
            return List.of();
        }

        // Resolve any generic T → actual type
        Type unfolded = context.resolve(type);
        Class<?> rawClass = TypeResolver.extractRawClass(unfolded);
        if (rawClass == null) {
            return List.of();
        }

        // Only domain model classes are expanded
        if (!isDomain(rawClass)) {
            return List.of();
        }

        // Detect immediate self-recursion cases
        if (isSelfRecursive(rawClass)) {
            return List.of(JsonProperty.reference(rawClass.getSimpleName()));
        }

        List<JsonProperty> props = new ArrayList<>();

        for (Field field : rawClass.getDeclaredFields()) {
            field.setAccessible(true);

            // Apply generics substitution at field-level as well
            Type fieldType = context.resolve(field.getGenericType());

            JsonProperty resolved = JsonPropertyResolver.resolve(field, fieldType, context);
            props.add(resolved);
        }

        return props;
    }

    /**
     * Determines whether the given class directly references itself in any field.
     *
     * <p>This detects:</p>
     * <ul>
     *   <li>A → A</li>
     *   <li>A → List&lt;A&gt;</li>
     *   <li>A → Set&lt;A&gt;</li>
     *   <li>A → A[]</li>
     *   <li>A → Map&lt;String, A&gt;</li>
     * </ul>
     *
     * @param clazz the class to inspect
     * @return true if direct self-recursion exists
     */
    private boolean isSelfRecursive(Class<?> clazz) {

        for (Field field : clazz.getDeclaredFields()) {

            Type fieldType = field.getGenericType();
            Class<?> raw = TypeResolver.extractRawClass(fieldType);

            // Case 1: direct self-reference (A → A)
            if (raw == clazz) {
                return true;
            }

            // Case 2: array of A
            if (raw != null && raw.isArray() && raw.getComponentType() == clazz) {
                return true;
            }

            // Case 3: collection of A (List<A>, Set<A>, etc.)
            if (raw != null && Collection.class.isAssignableFrom(raw)) {
                Type elem = TypeResolver.getTypeParameter(fieldType);
                if (TypeResolver.extractRawClass(elem) == clazz) {
                    return true;
                }
            }

            // Case 4: Map<?, A>
            if (raw != null && Map.class.isAssignableFrom(raw)) {
                Type valueType = TypeResolver.getMapValueType(fieldType);
                if (TypeResolver.extractRawClass(valueType) == clazz) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determines whether a class belongs to the consumer application's domain package(s).
     */
    private boolean isDomain(Class<?> c) {
        Package pkg = c.getPackage();
        if (pkg == null) return false;

        String p = pkg.getName();
        return SchemaConfig.getBasePackages().stream().anyMatch(p::startsWith);
    }
}
