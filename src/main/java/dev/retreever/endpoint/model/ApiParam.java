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
 * Represents a resolved query parameter for an endpoint.
 * Captures type, requirement flags, default values,
 * and any extracted validation constraints.
 */
public class ApiParam {

    private String name;
    private JsonPropertyType type;      // STRING, NUMBER, BOOLEAN...
    private boolean required;
    private String description;
    private String defaultValue;

    private final Set<String> constraints = new HashSet<>();

    // ─────── getters ───────

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

    public String getDefaultValue() {
        return defaultValue;
    }

    public Set<String> getConstraints() {
        return constraints;
    }

    // ─────── fluent setters ───────

    public ApiParam setName(String name) {
        this.name = name;
        return this;
    }

    public ApiParam setType(JsonPropertyType type) {
        this.type = type;
        return this;
    }

    public ApiParam setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public ApiParam setDescription(String description) {
        this.description = description;
        return this;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void addConstraint(String constraint) {
        this.constraints.add(constraint);
    }
}
