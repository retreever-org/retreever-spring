/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver.jackson;

import dev.retreever.json.RetreeverJsonMapper;
import dev.retreever.json.RetreeverJsonMappers;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

public class JsonNameResolver {

    private static final List<String> JSON_IGNORE_ANNOTATIONS = List.of(
            "com.fasterxml.jackson.annotation.JsonIgnore",
            "tools.jackson.annotation.JsonIgnore"
    );
    private static final List<String> JSON_PROPERTY_ANNOTATIONS = List.of(
            "com.fasterxml.jackson.annotation.JsonProperty",
            "tools.jackson.annotation.JsonProperty"
    );
    private static final List<String> JSON_GETTER_ANNOTATIONS = List.of(
            "com.fasterxml.jackson.annotation.JsonGetter",
            "tools.jackson.annotation.JsonGetter"
    );
    private static final List<String> JSON_SETTER_ANNOTATIONS = List.of(
            "com.fasterxml.jackson.annotation.JsonSetter",
            "tools.jackson.annotation.JsonSetter"
    );
    private static final List<String> JSON_NAMING_ANNOTATIONS = List.of(
            "com.fasterxml.jackson.databind.annotation.JsonNaming",
            "tools.jackson.databind.annotation.JsonNaming"
    );
    private static volatile RetreeverJsonMapper mapper = RetreeverJsonMappers.defaultMapper();

    public static void configure(RetreeverJsonMapper objectMapper) {
        if (objectMapper != null) {
            mapper = objectMapper;
        }
    }

    public static void configure(Object objectMapper) {
        if (objectMapper != null) {
            mapper = RetreeverJsonMappers.wrap(objectMapper);
        }
    }

    public static boolean isJsonIgnored(AnnotatedElement elem) {
        return hasAnnotation(elem, JSON_IGNORE_ANNOTATIONS);
    }

    /**
     * Resolves final JSON name for any AnnotatedElement.
     * @param elem Field/Method/Parameter
     * @param declaringClass Class with @JsonNaming (from elem.getDeclaringClass() or equivalent)
     * @param defaultName Raw Java name
     */
    public static String resolveJsonPropertyName(AnnotatedElement elem, Class<?> declaringClass, String defaultName) {
        // 1. Prefer Jackson's own serialization model.
        if (elem instanceof Field field) {
            String jacksonName = resolveJacksonPropertyName(field, declaringClass);
            if (jacksonName != null && !jacksonName.isBlank()) {
                return jacksonName;
            }
        }

        // 2. Highest fallback: @JsonProperty/@JsonGetter/@JsonSetter
        String name = getJsonPropertyNameOrElseDefault(elem, defaultName);

        // 3. If no explicit annotation, apply @JsonNaming
        if (name.equals(defaultName)) {
            Class<?> namingStrategy = resolveNamingStrategyClass(declaringClass);
            if (namingStrategy != null) {
                name = applyNamingStrategy(namingStrategy, defaultName);
            }
        }
        return name;
    }

    private static String resolveJacksonPropertyName(Field field, Class<?> declaringClass) {
        return mapper.resolvePropertyName(field, declaringClass);
    }

    private static String getJsonPropertyNameOrElseDefault(AnnotatedElement elem, String defaultName) {
        String explicitPropertyName = annotationValue(elem, JSON_PROPERTY_ANNOTATIONS);
        if (explicitPropertyName != null && !explicitPropertyName.isEmpty()) {
            return explicitPropertyName;
        }

        String getterName = annotationValue(elem, JSON_GETTER_ANNOTATIONS);
        if (getterName != null && !getterName.isEmpty()) {
            return getterName;
        }

        String setterName = annotationValue(elem, JSON_SETTER_ANNOTATIONS);
        if (setterName != null && !setterName.isEmpty()) {
            return setterName;
        }

        return defaultName;
    }

    private static String applyNamingStrategy(Class<?> strategyClass, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        String strategyClassName = strategyClass.getName();

        if (strategyClassName.endsWith("PropertyNamingStrategies$SnakeCaseStrategy")) {
            return camelToSnakeCase(lowerInput);
        }
        if (strategyClassName.endsWith("PropertyNamingStrategies$UpperSnakeCaseStrategy")) {
            return camelToSnakeCase(lowerInput).toUpperCase(Locale.ROOT);
        }
        if (strategyClassName.endsWith("PropertyNamingStrategies$KebabCaseStrategy")) {
            return camelToKebabCase(lowerInput);
        }
        if (strategyClassName.endsWith("PropertyNamingStrategies$UpperCamelCaseStrategy")) {
            return Character.toUpperCase(lowerInput.charAt(0)) + lowerInput.substring(1);
        }
        if (strategyClassName.endsWith("PropertyNamingStrategies$LowerCamelCaseStrategy")) {
            return lowerInput;
        }
        if (strategyClassName.endsWith("PropertyNamingStrategies$LowerDotCaseStrategy")) {
            return camelToSnakeCase(lowerInput).replace('_', '.');
        }

        return input;
    }

    private static String camelToSnakeCase(String camel) {
        StringBuilder snake = new StringBuilder(camel.length() * 2);
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) snake.append('_');
                snake.append(Character.toLowerCase(c));
            } else {
                snake.append(c);
            }
        }
        return snake.toString();
    }

    private static String camelToKebabCase(String camel) {
        return camelToSnakeCase(camel).replace('_', '-');
    }

    private static boolean hasAnnotation(AnnotatedElement element, List<String> annotationNames) {
        for (Annotation annotation : element.getAnnotations()) {
            if (annotationNames.contains(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private static String annotationValue(AnnotatedElement element, List<String> annotationNames) {
        for (Annotation annotation : element.getAnnotations()) {
            if (!annotationNames.contains(annotation.annotationType().getName())) {
                continue;
            }

            try {
                Method valueMethod = annotation.annotationType().getMethod("value");
                Object value = valueMethod.invoke(annotation);
                return value instanceof String stringValue ? stringValue : null;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Class<?> resolveNamingStrategyClass(Class<?> declaringClass) {
        for (Annotation annotation : declaringClass.getAnnotations()) {
            if (!JSON_NAMING_ANNOTATIONS.contains(annotation.annotationType().getName())) {
                continue;
            }

            try {
                Method valueMethod = annotation.annotationType().getMethod("value");
                Object value = valueMethod.invoke(annotation);
                return value instanceof Class<?> strategyClass ? strategyClass : null;
            } catch (ReflectiveOperationException ignored) {
                return null;
            }
        }

        return null;
    }

}
