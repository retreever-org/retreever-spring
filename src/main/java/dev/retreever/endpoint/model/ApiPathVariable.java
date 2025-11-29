/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.model;

import dev.retreever.schema.model.JsonPropertyType;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a resolved path variable from an endpoint URL.
 * Captures name, inferred type, description, and any constraints.
 */
public class ApiPathVariable {

    private String name;                    // e.g., "id"
    private JsonPropertyType type;          // STRING, NUMBER, etc.
    private String description;             // optional dev doc

    private final Set<String> constraints = new HashSet<>();

    // ───────── getters ─────────

    public String getName() {
        return name;
    }

    public JsonPropertyType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getConstraints() {
        return constraints;
    }

    // ───────── fluent setters ─────────

    public ApiPathVariable setName(String name) {
        this.name = name;
        return this;
    }

    public ApiPathVariable setType(JsonPropertyType type) {
        this.type = type;
        return this;
    }

    public ApiPathVariable setDescription(String description) {
        this.description = description;
        return this;
    }

    public void addConstraint(String constraint) {
        this.constraints.add(constraint);
    }
}
