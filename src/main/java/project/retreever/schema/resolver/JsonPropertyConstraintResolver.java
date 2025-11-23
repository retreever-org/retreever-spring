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

public class JsonPropertyConstraintResolver {

    public static void resolve(JsonProperty jsonProp, Field field) {
        Annotation[] anns = field.getAnnotations();

        Set<String> constraints = ConstraintResolver.resolve(anns);
        constraints.forEach(jsonProp::addConstraint);

        if (ConstraintResolver.isRequired(anns)) {
            jsonProp.required();
        }
    }

}
