/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.resolver;

import dev.retreever.domain.model.JsonProperty;

import java.lang.reflect.Type;
import java.util.List;

public class JsonSchemaResolver {

    public List<JsonProperty> resolve(Type type) {

        GenericContext context = new GenericContext();

        // Build type-variable bindings at top-level
        context.bind(type);

        JsonObjectResolver resolver = new JsonObjectResolver(context);

        return resolver.resolve(type);
    }

    public List<JsonProperty> resolve(Class<?> clazz) {
        return resolve((Type) clazz);
    }
}
