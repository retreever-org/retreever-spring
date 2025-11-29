package dev.retreever.schema.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a named field/property within a JSON object.
 */
public class Property implements Schema {

    private final String name; // field name
    private final JsonPropertyType type; // data type
    private final Schema value; // value schema

    private boolean required = false;
    private String description;
    private Object example;
    private final Set<String> constraints = new HashSet<>();

    public Property(String name, JsonPropertyType type, Schema value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }

    // --- Getters ---

    public String getName() {
        return name;
    }

    public JsonPropertyType getType() {
        return type;
    }

    public Schema getValue() {
        return value;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public Object getExample() {
        return example;
    }

    public Set<String> getConstraints() {
        return constraints;
    }

    // --- Mutators (fluent) ---

    public Property required() {
        this.required = true;
        return this;
    }

    public Property description(String desc) {
        this.description = desc;
        return this;
    }

    public Property example(Object ex) {
        this.example = ex;
        return this;
    }

    public void addConstraint(String constraint) {
        if (constraint != null && !constraint.isBlank()) {
            this.constraints.add(constraint);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Property{name=").append(name)
                .append(", type=").append(type != null ? type.name() : "null")
                .append(", value=").append(value != null ? value.toString() : "null");

        if (required) {
            sb.append(", required=true");
        }

        if (description != null && !description.isBlank()) {
            sb.append(", desc=").append(description);
        }

        if (example != null) {
            sb.append(", example=").append(example);
        }

        if (!constraints.isEmpty()) {
            sb.append(", constraints=[").append(String.join(", ", constraints)).append("]");
        }

        sb.append("}");
        return sb.toString();
    }

}
