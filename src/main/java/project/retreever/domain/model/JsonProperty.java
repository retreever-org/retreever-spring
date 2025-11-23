/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package project.retreever.domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a node in a JSON schema model.
 * Each JsonProperty can be:
 * - primitive (STRING, NUMBER, BOOLEAN, ENUM)
 * - object    (with nested properties)
 * - array     (with a single element schema)
 */
public class JsonProperty {

    private final String name;
    private final JsonPropertyType type;

    private boolean required = false;
    private String description;
    private Object exampleValue;

    private final Set<String> constraints = new HashSet<>();

    // For OBJECT types
    private List<JsonProperty> properties;

    // For ARRAY types
    private JsonProperty arrayElement;

    // ────────────────────────────────────────────
    // Constructors
    // ────────────────────────────────────────────

    private JsonProperty(String name, JsonPropertyType type) {
        this.name = name;
        this.type = type;

        if (type == JsonPropertyType.OBJECT) {
            this.properties = new ArrayList<>();
        }
    }

    public static JsonProperty of(String name, JsonPropertyType type) {
        return new JsonProperty(name, type);
    }

    // ────────────────────────────────────────────
    // Fluent API
    // ────────────────────────────────────────────

    public JsonProperty required() {
        this.required = true;
        return this;
    }

    public JsonProperty description(String desc) {
        this.description = desc;
        return this;
    }

    public void example(Object example) {
        this.exampleValue = example;
    }

    public void addConstraint(String constraint) {
        this.constraints.add(constraint);
    }

    public void addProperty(JsonProperty child) {
        if (this.type != JsonPropertyType.OBJECT) {
            throw new IllegalStateException("Cannot add properties to non-object type");
        }
        this.properties.add(child);
    }

    public void arrayElement(JsonProperty element) {
        if (this.type != JsonPropertyType.ARRAY) {
            throw new IllegalStateException("Cannot set array element on non-array type");
        }
        this.arrayElement = element;
    }

    // ────────────────────────────────────────────
    // Convenience Checks
    // ────────────────────────────────────────────

    public boolean isObject() {
        return type == JsonPropertyType.OBJECT;
    }

    public boolean isArray() {
        return type == JsonPropertyType.ARRAY;
    }

    // ────────────────────────────────────────────
    // Getters
    // ────────────────────────────────────────────

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

    public Object getExampleValue() {
        return exampleValue;
    }

    public Set<String> getConstraints() {
        return constraints;
    }

    public List<JsonProperty> getProperties() {
        return properties;
    }

    public JsonProperty getArrayElement() {
        return arrayElement;
    }
}
