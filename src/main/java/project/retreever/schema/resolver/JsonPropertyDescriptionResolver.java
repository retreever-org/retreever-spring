package project.retreever.schema.resolver;

import project.retreever.domain.annotation.Description;
import project.retreever.domain.annotation.FieldInfo;
import project.retreever.domain.model.JsonProperty;

import java.lang.reflect.AnnotatedElement;

public class JsonPropertyDescriptionResolver {

    /**
     * Resolves the description from the given annotated element (e.g., Field, Parameter).
     * If the @Description annotation is present, set the description on the given JsonProperty.
     *
     * @param jsonProp The JsonProperty to set description on
     * @param annotatedElement The element possibly annotated with @Description
     */
    public static void resolve(JsonProperty jsonProp, AnnotatedElement annotatedElement) {
        if (annotatedElement == null || jsonProp == null) {
            return;
        }

        Description descriptionAnnotation = annotatedElement.getAnnotation(Description.class);
        if (descriptionAnnotation != null) {
            jsonProp.description(descriptionAnnotation.value());
        } else {
            FieldInfo info = annotatedElement.getAnnotation(FieldInfo.class);
            if(info != null) {
                jsonProp.description(info.description());
            }
        }
    }
}
