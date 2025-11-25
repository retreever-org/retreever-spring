/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.repo;

import dev.retreever.domain.model.JsonProperty;
import dev.retreever.schema.resolver.JsonSchemaResolver;
import dev.retreever.schema.resolver.TypeResolver;

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
     * @param schema schema of the DTO
     * @param clazz  the class whose schema should be generated
     * @return reference name used as the registry key
     */
    public String registerSchema(List<JsonProperty> schema, Class<?> clazz) {
        String ref = TypeResolver.resolveRefName(clazz);
        add(ref, schema);
        return ref;
    }
}
