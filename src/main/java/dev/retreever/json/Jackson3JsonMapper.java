package dev.retreever.json;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

final class Jackson3JsonMapper implements RetreeverJsonMapper {

    private static final String JACKSON3_OBJECT_MAPPER = "tools.jackson.databind.ObjectMapper";

    private final Object mapper;

    Jackson3JsonMapper(Object mapper) {
        this.mapper = mapper;
    }

    static Jackson3JsonMapper createDefault() {
        try {
            Class<?> mapperClass = Class.forName(JACKSON3_OBJECT_MAPPER);
            Object mapper = mapperClass.getConstructor().newInstance();
            Object builder = invoke(mapper, "rebuild");
            invoke(builder, "findAndAddModules");
            return new Jackson3JsonMapper(invoke(builder, "build"));
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to initialize Jackson 3 ObjectMapper.", ex);
        }
    }

    @Override
    public RetreeverJsonMapper copyWithNonNullInclusion() {
        try {
            Object builder = invoke(mapper, "rebuild");
            return new Jackson3JsonMapper(invoke(builder, "build"));
        } catch (ReflectiveOperationException ex) {
            return this;
        }
    }

    @Override
    public byte[] writeValueAsBytes(Object value) throws IOException {
        try {
            return (byte[]) invoke(mapper, "writeValueAsBytes", Object.class, value);
        } catch (ReflectiveOperationException ex) {
            throw new IOException("Failed to serialize value with Jackson 3.", ex);
        }
    }

    @Override
    public <T> T readValue(byte[] value, Class<T> type) throws IOException {
        try {
            return type.cast(invoke(mapper, "readValue", byte[].class, value, Class.class, type));
        } catch (ReflectiveOperationException ex) {
            throw new IOException("Failed to deserialize value with Jackson 3.", ex);
        }
    }

    @Override
    public String resolvePropertyName(Field field, Class<?> declaringClass) {
        try {
            Object javaType = invoke(mapper, "constructType", Type.class, declaringClass);
            Object serializationConfig = invoke(mapper, "serializationConfig");
            Object beanDescription = invoke(serializationConfig, "introspect", javaType.getClass(), javaType);
            Object properties = invoke(beanDescription, "findProperties");

            if (properties instanceof List<?> list) {
                for (Object property : list) {
                    if (matchesField(property, field)) {
                        return (String) invoke(property, "getName");
                    }
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Fall through to annotation/naming strategy fallback in JsonNameResolver.
        }

        return null;
    }

    private boolean matchesField(Object property, Field field) throws ReflectiveOperationException {
        if (!(boolean) invoke(property, "couldSerialize")) {
            return false;
        }

        Object annotatedField = invoke(property, "getField");
        if (annotatedField != null) {
            Object underlyingField = invoke(annotatedField, "getAnnotated");
            if (field.equals(underlyingField)) {
                return true;
            }
        }

        String internalName = (String) invoke(property, "getInternalName");
        if (field.getName().equals(internalName)) {
            return true;
        }

        Object getter = invoke(property, "getGetter");
        if (getter != null && matchesAccessor((String) invoke(getter, "getName"), field)) {
            return true;
        }

        Object setter = invoke(property, "getSetter");
        return setter != null && matchesAccessor((String) invoke(setter, "getName"), field);
    }

    private boolean matchesAccessor(String accessorName, Field field) {
        String fieldName = field.getName();
        String accessorSuffix = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        return accessorName.equals(fieldName)
                || accessorName.equals("get" + fieldName)
                || accessorName.equals("get" + accessorSuffix)
                || accessorName.equals("is" + fieldName)
                || accessorName.equals("is" + accessorSuffix)
                || accessorName.equals("set" + fieldName)
                || accessorName.equals("set" + accessorSuffix);
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static Object invoke(Object target, String methodName, Class<?> parameterType, Object argument)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, parameterType);
        return method.invoke(target, argument);
    }

    private static Object invoke(
            Object target,
            String methodName,
            Class<?> firstParameterType,
            Object firstArgument,
            Class<?> secondParameterType,
            Object secondArgument) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, firstParameterType, secondParameterType);
        return method.invoke(target, firstArgument, secondArgument);
    }
}
