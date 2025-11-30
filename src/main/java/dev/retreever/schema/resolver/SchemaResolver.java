/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.schema.context.ResolverContext;
import dev.retreever.schema.model.JsonPropertyType;
import dev.retreever.schema.model.Schema;
import dev.retreever.schema.model.ValueSchema;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Central dispatcher for schema resolution using the instance-per-resolution pattern.
 * Delegates to specialized resolvers based on type classification.
 */
public class SchemaResolver {
    static final ThreadLocal<ResolverContext> CONTEXT =
            ThreadLocal.withInitial(ResolverContext::new);

    private static final ThreadLocal<Set<Type>> RESOLVING =
            ThreadLocal.withInitial(HashSet::new);

    private SchemaResolver() {}

    /**
     * Entry point for schema resolution with generic context initialization.
     */
    public static Schema initResolution(Type type) {
        CONTEXT.set(ResolverContext.fromRoot(type));
        try {
            return resolve(type);
        } finally {
            CONTEXT.remove();
            RESOLVING.get().clear();
        }
    }

    /**
     * Entry point used by all resolvers - handles substitution + recursion guard.
     */
    public static Schema resolve(Type type) {
        if (type == null) {
            return new ValueSchema(JsonPropertyType.NULL);
        }

        Set<Type> resolving = RESOLVING.get();
        if (!resolving.add(type)) {
            return new ValueSchema(JsonPropertyType.OBJECT);
        }

        try {
            Type resolvedType = CONTEXT.get().substitute(type);
            Class<?> rawType = extractRawClass(resolvedType);
            JsonPropertyType kind = JsonPropertyTypeResolver.resolve(rawType);

            return switch (kind) {
                case ARRAY -> ArraySchemaResolver.resolve(resolvedType);
                case OBJECT -> ObjectSchemaResolver.resolve(resolvedType);
                case MAP -> MapSchemaResolver.resolve(resolvedType);
                default -> ValueSchemaResolver.resolve(resolvedType);
            };
        } finally {
            resolving.remove(type);
        }
    }

    /**
     * Field resolution entry point - captures field-level generic context.
     * Used by ObjectSchemaResolver, ArraySchemaResolver for nested types.
     */
    public static Schema resolveField(Field field, Type declaringType, Type fieldType) {
        ResolverContext fieldCtx = ResolverContext.fromField(field, declaringType);
        ResolverContext mergedCtx = CONTEXT.get().merge(fieldCtx);

        CONTEXT.set(mergedCtx);
        try {
            return resolve(fieldType);
        } finally {
            CONTEXT.set(CONTEXT.get()); // Restore parent context
        }
    }

    /**
     * Extracts raw Class from any Type for classification.
     */
    public static Class<?> extractRawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType pt) {
            Type raw = pt.getRawType();
            if (raw instanceof Class<?> c) {
                return c;
            }
            return Object.class;
        } else if (type instanceof GenericArrayType) {
            return Object[].class;
        } else if (type instanceof TypeVariable<?>) {
            return Object.class;
        } else if (type instanceof WildcardType wt) {
            Type[] upper = wt.getUpperBounds();
            if (upper.length > 0 && upper[0] instanceof Class<?> c) {
                return c;
            }
            return Object.class;
        }
        return Object.class;
    }
}
