package project.retreever.endpoint.resolver;

import org.springframework.web.bind.annotation.PathVariable;
import project.retreever.domain.annotation.Description;
import project.retreever.domain.model.ApiEndpoint;
import project.retreever.domain.model.ApiPathVariable;
import project.retreever.domain.model.JsonPropertyType;
import project.retreever.schema.resolver.JsonPropertyTypeResolver;
import project.retreever.schema.resolver.util.ConstraintResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ApiPathVariableResolver {

    /**
     * Placeholder for resolving path variables (to be implemented later).
     */
    public static void resolvePathVariables(ApiEndpoint endpoint, Method method) {

        List<ApiPathVariable> vars = new ArrayList<>();

        for (Parameter param : method.getParameters()) {

            PathVariable pv = param.getAnnotation(PathVariable.class);
            if (pv == null) continue;

            ApiPathVariable var = new ApiPathVariable();

            // name
            String name = pv.name().isBlank()
                    ? (!pv.value().isBlank() ? pv.value() : param.getName())
                    : pv.name();

            var.setName(name);

            // type
            Class<?> rawType = param.getType();
            JsonPropertyType jsonType = JsonPropertyTypeResolver.resolve(rawType);
            var.setType(jsonType);

            // description
            Description desc = param.getAnnotation(Description.class);
            if (desc != null) {
                var.setDescription(desc.value());
            }

            // constraints
            Set<String> constraints =
                    ConstraintResolver.resolve(param.getAnnotations());
            constraints.forEach(var::addConstraint);

            vars.add(var);
        }

        endpoint.setPathVariables(vars);
    }
}
