/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import java.lang.reflect.*;
import java.util.Map;

/**
 * Utility methods for extracting raw classes and generic type parameters.
 * Used throughout schema resolution to unwrap {@link Type} instances
 * into concrete classes and generate reference names for nested generics.
 */
public class TypeResolver {

    /**
     * Produces a stable reference name for a type, including nested generic
     * structures (e.g. List<String> → "List.String").
     *
     * @param type the type to describe
     * @return simple name or a combined name for generic types
     */
    public static String resolveRefName(Type type) {
        if (type instanceof Class<?> clazz) {
            // Base case: non-generic class
            return clazz.getSimpleName();
        }

        if (type instanceof ParameterizedType p) {
            Type raw = p.getRawType();
            String rawName = ((Class<?>) raw).getSimpleName();

            Type[] args = p.getActualTypeArguments();
            if (args.length == 0) return rawName;

            StringBuilder sb = new StringBuilder(rawName);
            for (Type arg : args) {
                sb.append(".").append(resolveRefName(arg));
            }
            return sb.toString();
        }

        // Fallback for wildcard or type variable cases
        return type.getTypeName();
    }

    /**
     * Extracts the first type argument from a parameterized type.
     *
     * @param type the type to inspect
     * @return first type parameter or null if none
     */
    public static Type getTypeParameter(Type type) {
        if (type instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            return typeArgs.length > 0 ? typeArgs[0] : null;
        } else return null;
    }

    /**
     * Extracts the raw class from a {@link Type}, unwrapping parameterized types.
     *
     * @param type a class or parameterized type
     * @return the raw class, or null if not resolvable
     */
    public static Class<?> extractRawClass(Type type) {

        if (type instanceof Class<?> clazz) {
            return clazz;
        }

        if (type instanceof ParameterizedType pType) {
            Type raw = pType.getRawType();
            if (raw instanceof Class<?> rc) return rc;
        }

        if (type instanceof TypeVariable<?>) {
            return Object.class;
        }

        if (type instanceof WildcardType wc) {
            Type[] upper = wc.getUpperBounds();
            return extractRawClass(upper.length > 0 ? upper[0] : Object.class);
        }

        if (type instanceof GenericArrayType ga) {
            Type component = ga.getGenericComponentType();
            Class<?> compClass = extractRawClass(component);
            if (compClass != null) {
                return java.lang.reflect.Array.newInstance(compClass, 0).getClass();
            }
        }

        return null;
    }

    public static boolean isMap(Type type) {

        if (type instanceof Class<?> clazz) {
            return Map.class.isAssignableFrom(clazz);
        }

        if (type instanceof ParameterizedType p) {
            Type raw = p.getRawType();
            if (raw instanceof Class<?> clazz) {
                return Map.class.isAssignableFrom(clazz);
            }
        }

        return false;
    }

    /**
     * Extracts the value type V from Map<K, V>.
     * For raw maps, return Object.class.
     */
    public static Type getMapValueType(Type type) {

        // Map without generics → treat as Map<String, Object>
        if (!(type instanceof ParameterizedType p)) {
            return Object.class;
        }

        Type[] args = p.getActualTypeArguments();
        if (args.length != 2) {
            return Object.class; // defensive fallback
        }

        return args[1];
    }
}
