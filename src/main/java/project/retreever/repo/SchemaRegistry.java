/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.repo;

import project.retreever.domain.model.JsonProperty;
import project.retreever.schema.resolver.JsonSchemaResolver;
import project.retreever.schema.resolver.TypeResolver;

import java.util.List;

/**
 * Registry for storing resolved JSON schemas keyed by reference name.
 * Ensures each DTO type is processed once, returning a stable schema
 * reference for reuse across endpoints.
 */
public class SchemaRegistry extends DocRegistry<List<JsonProperty>> {

    private final JsonSchemaResolver jsonSchemaResolver;

    public SchemaRegistry(JsonSchemaResolver jsonSchemaResolver) {
        this.jsonSchemaResolver = jsonSchemaResolver;
    }

    /**
     * Resolves and registers the schema for the given DTO class if not already present.
     *
     * @param dtoClass the class whose schema should be generated
     * @return reference name used as the registry key
     */
    public String registerSchema(Class<?> dtoClass) {
        String ref = TypeResolver.resolveRefName(dtoClass);

        if (!contains(ref)) {
            List<JsonProperty> props = jsonSchemaResolver.resolve(dtoClass);
            add(ref, props);
        }

        return ref;
    }
}
