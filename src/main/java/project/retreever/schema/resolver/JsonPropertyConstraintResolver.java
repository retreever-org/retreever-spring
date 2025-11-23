/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.schema.resolver;

import project.retreever.domain.model.JsonProperty;
import project.retreever.schema.resolver.util.ConstraintResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

/**
 * Applies validation-based constraints to a {@link JsonProperty}.
 * Delegates extraction logic to {@link ConstraintResolver} and
 * marks properties as required when applicable.
 */
public class JsonPropertyConstraintResolver {

    /**
     * Populates constraint markers on the given JsonProperty
     * based on the validation annotations declared on the field.
     *
     * @param jsonProp the property being enriched
     * @param field    the source field inspected via reflection
     */
    public static void resolve(JsonProperty jsonProp, Field field) {
        Annotation[] anns = field.getAnnotations();

        Set<String> constraints = ConstraintResolver.resolve(anns);
        constraints.forEach(jsonProp::addConstraint);

        if (ConstraintResolver.isRequired(anns)) {
            jsonProp.required();
        }
    }
}
