package project.retreever.endpoint.resolver;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import project.retreever.domain.model.ApiError;
import project.retreever.domain.model.JsonProperty;
import project.retreever.schema.resolver.JsonSchemaResolver;
import project.retreever.schema.resolver.TypeResolver;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves ApiError objects from a list of methods.
 * Only @ExceptionHandler methods are processed.
 */
public class ApiErrorResolver {

    private final JsonSchemaResolver jsonSchemaResolver;

    public ApiErrorResolver(JsonSchemaResolver jsonSchemaResolver) {
        this.jsonSchemaResolver = jsonSchemaResolver;
    }

    /**
     * Accepts a list of methods and returns ApiError objects
     * for all methods annotated with @ExceptionHandler.
     */
    public List<ApiError> resolve(List<Method> methods) {

        List<ApiError> result = new ArrayList<>();

        for (Method method : methods) {

            ExceptionHandler handlerAnn = method.getAnnotation(ExceptionHandler.class);
            if (handlerAnn == null) {
                continue; // skip non-handlers
            }

            // Each declared exception type produces one ApiError entry
            Class<?>[] exceptionTypes = handlerAnn.value();
            if (exceptionTypes.length == 0) {
                continue;
            }

            // Resolve error schema from return type (once per handler)
            Class<?> returnType = extractReturnType(method);
            List<JsonProperty> resolvedSchema = resolveErrorSchema(returnType);

            // Read optional @ApiError annotation
            project.retreever.domain.annotation.ApiError apiAnn =
                    method.getAnnotation(project.retreever.domain.annotation.ApiError.class);

            HttpStatus status = (apiAnn != null)
                    ? apiAnn.status()
                    : HttpStatus.INTERNAL_SERVER_ERROR;

            String description = (apiAnn != null)
                    ? apiAnn.description()
                    : "";

            for (Class<?> exceptionType : exceptionTypes) {

                ApiError error = ApiError.create(
                        status,
                        description,
                        exceptionType.getName()
                );

                if (!resolvedSchema.isEmpty()) {
                    error.setErrorBody(resolvedSchema);
                }

                result.add(error);
            }
        }

        return result;
    }

    private List<JsonProperty> resolveErrorSchema(Class<?> type) {
        if (type == null || type == Void.class || type == void.class) {
            return List.of();
        }
        return jsonSchemaResolver.resolve(type);
    }

    private Class<?> extractReturnType(Method method) {

        var generic = method.getGenericReturnType();
        Class<?> raw = TypeResolver.extractRawClass(generic);

        if (raw == ResponseEntity.class && generic instanceof ParameterizedType p) {
            var inner = p.getActualTypeArguments()[0];
            return TypeResolver.extractRawClass(inner);
        }

        return raw;
    }
}
