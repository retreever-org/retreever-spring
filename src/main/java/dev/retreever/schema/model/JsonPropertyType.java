/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.schema.model;

/**
 * Supported JSON value types used when building schema properties.
 */
public enum JsonPropertyType {
    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    NULL("null"),
    OBJECT("object"),
    ARRAY("array"),
    MAP("map"),
    ENUM("enum"),
    UUID("uuid"),
    DATE("date"),
    TIME("time"),
    DATE_TIME("date-time"),
    BINARY("binary"),
    URI("uri"),
    DURATION("duration"),
    PERIOD("period");

    private final String displayName;

    JsonPropertyType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
