/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import java.lang.Class;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.Locale;

public class JsonNameResolver {

    private static volatile ObjectMapper mapper = new ObjectMapper();

    public static void configure(ObjectMapper objectMapper) {
        if (objectMapper != null) {
            mapper = objectMapper;
        }
    }

    public static boolean isJsonIgnored(AnnotatedElement elem) {
        return elem.isAnnotationPresent(JsonIgnore.class);
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
            JsonNaming naming = declaringClass.getAnnotation(JsonNaming.class);
            if (naming != null) {
                name = applyNamingStrategy(naming.value(), defaultName);
            }
        }
        return name;
    }

    private static String resolveJacksonPropertyName(Field field, Class<?> declaringClass) {
        ObjectMapper activeMapper = mapper;
        JavaType javaType = activeMapper.constructType(declaringClass);
        BeanDescription beanDesc = activeMapper.getSerializationConfig().introspect(javaType);

        for (BeanPropertyDefinition prop : beanDesc.findProperties()) {
            if (matchesField(prop, field)) {
                return prop.getName();
            }
        }

        return null;
    }

    private static boolean matchesField(BeanPropertyDefinition prop, Field field) {
        if (!prop.couldSerialize()) {
            return false;
        }

        if (prop.getField() != null && prop.getField().getAnnotated().equals(field)) {
            return true;
        }

        if (prop.getInternalName().equals(field.getName())) {
            return true;
        }

        if (prop.getGetter() != null && matchesAccessor(prop.getGetter().getName(), field)) {
            return true;
        }

        return prop.getSetter() != null && matchesAccessor(prop.getSetter().getName(), field);
    }

    private static boolean matchesAccessor(String accessorName, Field field) {
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

    private static String getJsonPropertyNameOrElseDefault(AnnotatedElement elem, String defaultName) {
        // Priority: @JsonProperty.value() > default
        JsonProperty prop = elem.getAnnotation(JsonProperty.class);
        if (prop != null && !prop.value().isEmpty()) return prop.value();

        return defaultName;
    }

    private static String applyNamingStrategy(Class<? extends PropertyNamingStrategy> strategyClass, String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);

        if (strategyClass == PropertyNamingStrategies.SnakeCaseStrategy.class) {
            return camelToSnakeCase(lowerInput);
        }
        if (strategyClass == PropertyNamingStrategies.UpperSnakeCaseStrategy.class) {
            return camelToSnakeCase(lowerInput).toUpperCase(Locale.ROOT);
        }
        if (strategyClass == PropertyNamingStrategies.KebabCaseStrategy.class) {
            return camelToKebabCase(lowerInput);
        }
        if (strategyClass == PropertyNamingStrategies.UpperCamelCaseStrategy.class) {
            return Character.toUpperCase(lowerInput.charAt(0)) + lowerInput.substring(1);
        }
        if (strategyClass == PropertyNamingStrategies.LowerCamelCaseStrategy.class) {
            return lowerInput;  // Already lower
        }
        if (strategyClass == PropertyNamingStrategies.LowerDotCaseStrategy.class) {
            return camelToSnakeCase(lowerInput).replace('_', '.');
        }

        return input;  // Default/unknown
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

}
