package project.retreever.schema.resolver;

import project.retreever.domain.model.JsonProperty;
import project.retreever.schema.resolver.util.ConstraintResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

public class JsonPropertyConstraintResolver {

    public static void resolve(JsonProperty jsonProp, Field field) {
        Annotation[] anns = field.getAnnotations();

        Set<String> constraints = ConstraintResolver.resolve(anns);
        constraints.forEach(jsonProp::addConstraint);

        if (ConstraintResolver.isRequired(anns)) {
            jsonProp.required();
        }
    }

}
