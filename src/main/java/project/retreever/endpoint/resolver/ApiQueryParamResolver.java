package project.retreever.endpoint.resolver;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import project.retreever.domain.annotation.Description;
import project.retreever.domain.model.ApiEndpoint;
import project.retreever.domain.model.ApiParam;
import project.retreever.domain.model.JsonPropertyType;
import project.retreever.schema.resolver.JsonPropertyTypeResolver;
import project.retreever.schema.resolver.util.ConstraintResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ApiQueryParamResolver {

    /**
     * Placeholder for resolving query parameters (to be implemented later).
     */
    public static void resolveQueryParams(ApiEndpoint endpoint, Method method) {

        List<ApiParam> params = new ArrayList<>();

        for (Parameter param : method.getParameters()) {

            RequestParam rp = param.getAnnotation(RequestParam.class);
            if (rp == null) continue;

            ApiParam qp = new ApiParam();

            // name
            String name = rp.name().isBlank()
                    ? (!rp.value().isBlank() ? rp.value() : param.getName())
                    : rp.name();
            qp.setName(name);

            // type
            Class<?> raw = param.getType();
            JsonPropertyType jsonType = JsonPropertyTypeResolver.resolve(raw);
            qp.setType(jsonType);

            // required
            qp.setRequired(rp.required());

            // default value
            if (!rp.defaultValue().equals(ValueConstants.DEFAULT_NONE)) {
                qp.setDefaultValue(rp.defaultValue());
            }

            // description from @Description
            Description desc = param.getAnnotation(Description.class);
            if (desc != null) {
                qp.setDescription(desc.value());
            }

            // constraints via ConstraintResolver
            Set<String> constraints = ConstraintResolver.resolve(param.getAnnotations());
            constraints.forEach(qp::addConstraint);

            params.add(qp);
        }

        endpoint.setQueryParams(params);
    }
}
