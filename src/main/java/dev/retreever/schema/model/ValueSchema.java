package dev.retreever.schema.model;

/**
 * Represents an atomic JSON value type.
 */
public class ValueSchema implements Schema {

    private final JsonPropertyType type;

    public ValueSchema(JsonPropertyType type) {
        this.type = type;
    }

    public JsonPropertyType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ValueSchema{" + type.name() + "}";
    }
}
