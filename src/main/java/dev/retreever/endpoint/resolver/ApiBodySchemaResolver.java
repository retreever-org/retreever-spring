/*
 * Copyright (c) 2025 Retreever Contributors
 *
 * Licensed under the MIT License.
 * You may obtain a copy of the License at:
 *     https://opensource.org/licenses/MIT
 */

package dev.retreever.endpoint.resolver;

import dev.retreever.domain.model.ApiEndpoint;
import dev.retreever.domain.model.JsonProperty;
import dev.retreever.schema.resolver.JsonSchemaResolver;
import dev.retreever.repo.SchemaRegistry;
import dev.retreever.schema.resolver.TypeResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Resolves request and response body schema references for controller methods.
 * <p>
 * This implementation preserves full generic type information end-to-end.
 * Wrapper types such as ResponseEntity<T>, ApiResponse<T>, PageResponse<T>,
 * and deeply nested generics (A<B<C<D>>>) are resolved accurately.
 * <p>
 * <b>Rules:</b>
 * <ul>
 *   <li>RequestBody → use parameter.getParameterizedType()</li>
 *   <li>ResponseBody → use method.getGenericReturnType()</li>
 *   <li>NEVER collapse Type → Class (keeps T intact)</li>
 *   <li>Schema registry receives raw class only for naming</li>
 * </ul>
 */
public class ApiBodySchemaResolver {

    private final SchemaRegistry schemaRegistry;
    private final JsonSchemaResolver jsonSchemaResolver;

    public ApiBodySchemaResolver(
            SchemaRegistry schemaRegistry,
            JsonSchemaResolver jsonSchemaResolver
    ) {
        this.schemaRegistry = schemaRegistry;
        this.jsonSchemaResolver = jsonSchemaResolver;
    }

    /**
     * Main entry point for attaching request/response schema references
     * to an {@link ApiEndpoint}.
     */
    public void resolve(ApiEndpoint endpoint, Method method) {
        resolveRequestSchema(endpoint, method);
        resolveResponseSchema(endpoint, method);
    }

    // REQUEST BODY
    private void resolveRequestSchema(ApiEndpoint endpoint, Method method) {

        Parameter body = findRequestBodyParameter(method);

        // No @RequestBody found
        if (body == null) {
            endpoint.setRequestBody(null);
            return;
        }

        // IMPORTANT: Never convert to Class. Keep Type as-is.
        Type requestType = body.getParameterizedType();

        setSchemaRef(endpoint, requestType, true);
    }

    private Parameter findRequestBodyParameter(Method method) {
        for (Parameter p : method.getParameters()) {
            if (p.isAnnotationPresent(RequestBody.class)) {
                return p;
            }
        }
        return null;
    }

    // RESPONSE BODY
    private void resolveResponseSchema(ApiEndpoint endpoint, Method method) {

        Type returnType = method.getGenericReturnType();  // full generic type

        // Unwrap ResponseEntity<T>
        if (returnType instanceof ParameterizedType p) {
            Class<?> raw = TypeResolver.extractRawClass(p.getRawType());
            if (raw == ResponseEntity.class) {
                returnType = p.getActualTypeArguments()[0]; // unwrap inner
            }
        }

        setSchemaRef(endpoint, returnType, false);
    }

    // SCHEMA REGISTRATION
    private void setSchemaRef(ApiEndpoint endpoint, Type type, boolean isRequest) {

        // Resolve JSON schema with full generic type preserved
        List<JsonProperty> schema = jsonSchemaResolver.resolve(type);

        // Schema registry uses raw class only for naming
        Class<?> raw = TypeResolver.extractRawClass(type);

        String ref = schemaRegistry.registerSchema(schema, raw);

        if (isRequest) {
            endpoint.setRequestBody(ref);
        } else {
            endpoint.setResponseBody(ref);
        }
    }

    // UTIL
    private boolean isPrimitiveLike(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || CharSequence.class.isAssignableFrom(type)
                || type == Boolean.class
                || type.isEnum();
    }
}
