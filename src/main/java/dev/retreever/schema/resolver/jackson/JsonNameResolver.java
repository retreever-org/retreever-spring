/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver.jackson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.lang.Class;
import java.lang.reflect.AnnotatedElement;
import java.util.Locale;

public class JsonNameResolver {

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
        // 1. Highest: @JsonProperty/@JsonGetter/@JsonSetter
        String name = getJsonPropertyNameOrElseDefault(elem, defaultName);

        // 2. If no explicit annotation, apply @JsonNaming
        if (name.equals(defaultName)) {
            JsonNaming naming = declaringClass.getAnnotation(JsonNaming.class);
            if (naming != null) {
                name = applyNamingStrategy(naming.value(), defaultName);
            }
        }
        return name;
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
