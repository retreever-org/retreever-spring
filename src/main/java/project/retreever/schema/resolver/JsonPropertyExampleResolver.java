/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.schema.resolver;

import project.retreever.domain.annotation.FieldInfo;
import project.retreever.domain.model.JsonProperty;

import java.lang.reflect.Field;

/**
 * Resolves example values for a {@link JsonProperty} from {@link FieldInfo}.
 * If the annotation declares a non-empty example, it is applied to the property.
 */
public class JsonPropertyExampleResolver {

    /**
     * Applies an example value to the given JSON property, if the field
     * contains a {@link FieldInfo} annotation with a defined example.
     *
     * @param property the property to update
     * @param field    the field annotated with FieldInfo
     */
    public static void resolve(JsonProperty property, Field field) {
        if (property == null || field == null) {
            return;
        }

        FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
        if (fieldInfo != null) {
            String example = fieldInfo.example();
            if (example != null && !example.isBlank()) {
                property.example(example);
            }
        }
    }
}
