/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.annotation.Description;
import dev.retreever.annotation.FieldInfo;
import dev.retreever.schema.model.Property;

import java.lang.reflect.AnnotatedElement;

/**
 * Resolves human-readable descriptions for a {@link Property}.
 * Looks for {@link Description} or {@link FieldInfo} annotations on
 * fields or parameters and applies the associated text.
 */
public class PropertyDescriptionResolver {

    /**
     * Applies description metadata from annotations declared on the
     * provided element. Checks @Description first, then @FieldInfo.
     *
     * @param prop         the target JSON property
     * @param annotatedElement the element annotated with description metadata
     */
    public static void resolve(Property prop, AnnotatedElement annotatedElement) {
        if (annotatedElement == null || prop == null) {
            return;
        }

        Description descriptionAnnotation = annotatedElement.getAnnotation(Description.class);
        if (descriptionAnnotation != null) {
            prop.description(descriptionAnnotation.value());
        } else {
            FieldInfo info = annotatedElement.getAnnotation(FieldInfo.class);
            if (info != null) {
                prop.description(info.description());
            }
        }
    }
}
