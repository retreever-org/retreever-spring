package dev.retreever.schema.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a JSON object (set of named properties).
 * Properties are stored in insertion order.
 */
public class ObjectSchema implements Schema {

    private final Map<String, Property> properties = new LinkedHashMap<>();

    public void addProperty(Property property) {
        if (property == null) return;
        properties.put(property.getName(), property);
    }

    public void addProperties(Map<String, Property> props) {
        if (props == null) return;
        props.values().forEach(this::addProperty);
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ObjectSchema{");

        if (properties.isEmpty()) {
            sb.append("empty");
        } else {
            properties.values().forEach(prop ->
                    sb.append(prop.getName()).append("=").append(prop).append(", ")
            );
            // Remove trailing comma and space
            if (!properties.isEmpty()) {
                sb.setLength(sb.length() - 2);
            }
        }

        sb.append("}");
        return sb.toString();
    }

}
