/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.schema.resolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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

        if (type instanceof ParameterizedType paramType) {
            Type rawType = paramType.getRawType();
            String rawTypeName = ((Class<?>) rawType).getSimpleName();

            Type t = getTypeParameter(type);
            return rawTypeName + "." + resolveRefName(t);
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
        } else if (type instanceof ParameterizedType pType) {
            Type rawType = pType.getRawType();
            if (rawType instanceof Class<?> rawClazz) {
                return rawClazz;
            }
        }
        return null;
    }
}
