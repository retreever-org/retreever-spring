package project.retreever.schema.resolver;

import project.retreever.domain.annotation.FieldInfo;
import project.retreever.domain.model.JsonProperty;

import java.lang.reflect.Field;

public class JsonPropertyExampleResolver {

    /**
     * Resolves and sets the example value of a JsonProperty from the @FieldInfo annotation on the field,
     * if present and non-empty. If not present or blank, no example is set.
     *
     * @param property The JsonProperty to set the example on
     * @param field The field annotated potentially with @FieldInfo
     */
    public static void resolve(JsonProperty property, Field field) {
        if (property == null || field == null) {
            return;
        }

        FieldInfo fieldInfo = field.getAnnotation(FieldInfo.class);
        if (fieldInfo != null) {
            String example = fieldInfo.example();
            if (example != null && !example.isBlank()) {
                property.example(example);
            }
        }
    }
}
