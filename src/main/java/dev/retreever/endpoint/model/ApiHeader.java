/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.model;

import dev.retreever.schema.model.JsonPropertyType;

/**
 * Represents a resolved HTTP header used by an endpoint.
 * Includes name, type, requirement flag, and optional description.
 */
public class ApiHeader {

    private String name;                   // e.g. Authorization
    private JsonPropertyType type;         // STRING, NUMBER, BOOLEAN...
    private boolean required;              // required header or optional
    private String description;            // developer-provided doc

    // ───────── Getters ─────────

    public String getName() {
        return name;
    }

    public JsonPropertyType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    // ───────── Fluent Setters ─────────

    public ApiHeader setName(String name) {
        this.name = name;
        return this;
    }

    public ApiHeader setType(JsonPropertyType type) {
        this.type = type;
        return this;
    }

    public ApiHeader setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public ApiHeader setDescription(String description) {
        this.description = description;
        return this;
    }
}
