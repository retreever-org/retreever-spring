/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.model;

import java.util.Objects;

/**
 * Schema representation for Map&lt;K,V&gt; types.
 * Stores key type and value schema for generic Maps.
 */
public class MapSchema implements Schema {

    private final JsonPropertyType keyType;
    private final Schema valueSchema;

    public MapSchema(JsonPropertyType keyType, Schema valueSchema) {
        this.keyType = Objects.requireNonNull(keyType, "keyType cannot be null");
        this.valueSchema = Objects.requireNonNull(valueSchema, "valueSchema cannot be null");
    }

    public JsonPropertyType getKeyType() {
        return keyType;
    }

    public Schema getValueSchema() {
        return valueSchema;
    }

    @Override
    public String toString() {
        return "MapSchema[key=" + keyType + ", value=" + valueSchema + "]";
    }
}
