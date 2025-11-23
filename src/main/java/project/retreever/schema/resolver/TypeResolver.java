package project.retreever.schema.resolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeResolver {

    public static String resolveRefName(Type type) {
        if (type instanceof Class<?> clazz) {
            // Base case: non-generic class
            return clazz.getSimpleName();
        }

        if (type instanceof ParameterizedType paramType) {
            // Get raw type simple name
            Type rawType = paramType.getRawType();
            String rawTypeName = ((Class<?>) rawType).getSimpleName();

            Type t = getTypeParameter(type);
            return rawTypeName + "." + resolveRefName(t);
        }

        // Other type handling (WildcardType, TypeVariable, etc.) - fallback
        return type.getTypeName();
    }

    public static Type getTypeParameter(Type type) {
        if (type instanceof ParameterizedType paramType) {
            Type[] typeArgs = paramType.getActualTypeArguments();
            return typeArgs.length > 0 ? typeArgs[0] : null;
        } else return null;
    }

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
