package dev.retreever.endpoint.resolver;

import dev.retreever.endpoint.model.ApiEndpoint;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

/**
 * Collects declared request/response Types for schema generation.
 * No resolving, no unwrapping, no registry interaction.
 * SchemaResolver handles everything later.
 */
public class ApiBodySchemaResolver {

    public void resolve(ApiEndpoint endpoint, Method method) {
        extractRequestType(endpoint, method);
        extractResponseType(endpoint, method);
    }

    // REQUEST -------------------------------------------------------

    private void extractRequestType(ApiEndpoint endpoint, Method method) {

        Parameter body = findRequestBodyParameter(method);

        if (body == null) {
            endpoint.setRequestBodyType(null);
            return;
        }

        Type t = body.getParameterizedType(); // keep generics intact
        endpoint.setRequestBodyType(t);
    }

    private Parameter findRequestBodyParameter(Method method) {
        for (Parameter p : method.getParameters()) {
            if (p.isAnnotationPresent(RequestBody.class)) return p;
        }
        return null;
    }

    // RESPONSE ------------------------------------------------------

    private void extractResponseType(ApiEndpoint endpoint, Method method) {
        Type t = method.getGenericReturnType(); // full generic type
        endpoint.setResponseBodyType(t);
    }
}
