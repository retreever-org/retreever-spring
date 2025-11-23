/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.schema.resolver;

import project.retreever.domain.annotation.Description;
import project.retreever.domain.annotation.FieldInfo;
import project.retreever.domain.model.JsonProperty;

import java.lang.reflect.AnnotatedElement;

/**
 * Resolves human-readable descriptions for a {@link JsonProperty}.
 * Looks for {@link Description} or {@link FieldInfo} annotations on
 * fields or parameters and applies the associated text.
 */
public class JsonPropertyDescriptionResolver {

    /**
     * Applies description metadata from annotations declared on the
     * provided element. Checks @Description first, then @FieldInfo.
     *
     * @param jsonProp         the target JSON property
     * @param annotatedElement the element annotated with description metadata
     */
    public static void resolve(JsonProperty jsonProp, AnnotatedElement annotatedElement) {
        if (annotatedElement == null || jsonProp == null) {
            return;
        }

        Description descriptionAnnotation = annotatedElement.getAnnotation(Description.class);
        if (descriptionAnnotation != null) {
            jsonProp.description(descriptionAnnotation.value());
        } else {
            FieldInfo info = annotatedElement.getAnnotation(FieldInfo.class);
            if (info != null) {
                jsonProp.description(info.description());
            }
        }
    }
}
