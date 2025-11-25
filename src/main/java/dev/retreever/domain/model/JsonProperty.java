/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a single JSON schema-like property.
 *
 * <p>Supports primitives, objects with nested properties,
 * arrays with a defined element type, and reference
 * placeholders used to safely represent recursive structures.</p>
 */
public class JsonProperty {

    private final String name;
    private final JsonPropertyType type;

    private boolean required = false;
    private String description;
    private Object exampleValue;

    private final Set<String> constraints = new HashSet<>();

    /** Nested properties for OBJECT types. */
    private List<JsonProperty> properties;

    /** Element schema for ARRAY types. */
    private JsonProperty arrayElement;

    /**
     * Optional reference to another schema type. Used when the
     * resolver detects self-referencing or cyclic object graphs.
     */
    private String ref;

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

    /**
     * Creates a reference node pointing to another type.
     * Used for recursion-safe schema output.
     *
     * @param refName the target type name
     * @return JsonProperty marked as a reference
     */
    public static JsonProperty reference(String refName) {
        JsonProperty prop = new JsonProperty(refName, JsonPropertyType.OBJECT);
        prop.ref = refName;
        return prop;
    }

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

    public void addObjectProperty(List<JsonProperty> object) {
        if (this.type != JsonPropertyType.OBJECT) {
            throw new IllegalStateException("Cannot add properties to non-object type");
        }
        this.properties.addAll(object);
    }

    public void arrayElement(JsonProperty element) {
        if (this.type != JsonPropertyType.ARRAY) {
            throw new IllegalStateException("Cannot set array element on non-array type");
        }
        this.arrayElement = element;
    }

    public boolean isObject() {
        return type == JsonPropertyType.OBJECT;
    }

    public boolean isArray() {
        return type == JsonPropertyType.ARRAY;
    }

    public boolean isReference() {
        return ref != null;
    }

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

    public String getRef() {
        return ref;
    }
}
