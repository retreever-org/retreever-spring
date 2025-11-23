package project.retreever.endpoint.resolver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import project.retreever.domain.model.ApiEndpoint;
import project.retreever.repo.SchemaRegistry;
import project.retreever.schema.resolver.TypeResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;

/**
 * Resolves request and response body schema references for a controller method.
 * Registers all resolved schema's to the registry.
 */
public class ApiBodySchemaResolver {

    private final SchemaRegistry schemaRegistry;

    public ApiBodySchemaResolver(SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    /**
     * Entry point for resolving request and response body schema references.
     */
    public void resolve(ApiEndpoint endpoint, Method method) {
        resolveRequestSchema(endpoint, method);
        resolveResponseSchema(endpoint, method);
    }

    private void resolveRequestSchema(ApiEndpoint endpoint, Method method) {

        Parameter reqBodyParam = findRequestBodyParameter(method);
        boolean consumesJson = endpoint.getConsumes()
                .stream().anyMatch(c -> c.equalsIgnoreCase("application/json"));

        if (consumesJson && reqBodyParam == null) {
            endpoint.setRequestBody(null);
            return;
        }

        if (reqBodyParam != null) {
            Class<?> type = extractClass(reqBodyParam.getParameterizedType());
            setSchemaRef(endpoint, type, true);
            return;
        }

        Class<?> complexType = findSingleComplexParameter(method);
        if (complexType != null) {
            setSchemaRef(endpoint, complexType, true);
            return;
        }

        endpoint.setRequestBody(null);
    }

    private void resolveResponseSchema(ApiEndpoint endpoint, Method method) {

        Class<?> returnType = extractReturnClass(method);

        if (returnType == null || returnType == Void.class || returnType == void.class) {
            endpoint.setResponseBody(null);
            return;
        }

        setSchemaRef(endpoint, returnType, false);
    }

    private void setSchemaRef(ApiEndpoint endpoint, Class<?> clazz, boolean isRequest) {
        String ref = schemaRegistry.registerSchema(clazz);
        if (isRequest) {
            endpoint.setRequestBody(ref);
        } else {
            endpoint.setResponseBody(ref);
        }
    }

    private Parameter findRequestBodyParameter(Method method) {
        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(RequestBody.class)) {
                return param;
            }
        }
        return null;
    }

    private Class<?> extractReturnClass(Method method) {

        java.lang.reflect.Type type = method.getGenericReturnType();
        Class<?> raw = extractClass(type);

        if (raw == ResponseEntity.class && type instanceof ParameterizedType p) {
            java.lang.reflect.Type inner = p.getActualTypeArguments()[0];
            return extractClass(inner);
        }

        return raw;
    }

    private Class<?> findSingleComplexParameter(Method method) {

        Class<?> found = null;

        for (Parameter param : method.getParameters()) {
            if (param.getAnnotations().length > 0) continue;

            Class<?> type = extractClass(param.getParameterizedType());

            if (isPrimitiveLike(type)) continue;
            if (found != null) return null;

            found = type;
        }

        return found;
    }

    private Class<?> extractClass(java.lang.reflect.Type type) {
        return TypeResolver.extractRawClass(type);
    }

    private boolean isPrimitiveLike(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || type == Boolean.class
                || type.isEnum();
    }
}
