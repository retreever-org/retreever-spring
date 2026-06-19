package dev.retreever.endpoint.resolver;

import dev.retreever.endpoint.model.ApiEndpoint;
import dev.retreever.repo.ApiHeaderRegistry;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Builds a dev.retreever.endpoint.model.ApiEndpoint from a controller method.
 * NO schema resolving happens here.
 * Only metadata + Java Types.
 */
public class ApiEndpointResolver {

    private final ApiEndpointIOResolver ioResolver;
    private final StringValueResolver valueResolver;

    public ApiEndpointResolver(
            ApiHeaderRegistry headerRegistry
    ) {
        this(headerRegistry, null);
    }

    public ApiEndpointResolver(
            ApiHeaderRegistry headerRegistry,
            StringValueResolver valueResolver
    ) {
        this.ioResolver = new ApiEndpointIOResolver(headerRegistry);
        this.valueResolver = valueResolver;
    }

    public ApiEndpoint resolve(Method method) {

        ApiEndpoint ep = new ApiEndpoint();

        // 1. Metadata
        EndpointMetadataResolver.resolve(ep, method);

        // 2. Path + HTTP method
        EndpointPathAndMethodResolver.resolve(ep, method, valueResolver);

        // 3. Consumes / Produces
        EndpointContentTypeResolver.resolve(ep, method);

        // 4. Types, params, headers
        ioResolver.resolve(ep, method);

        // 5. Error types (NO resolving here, only store Types)
        dev.retreever.annotation.ApiEndpoint ann =
                method.getAnnotation(dev.retreever.annotation.ApiEndpoint.class);

        if (ann != null && ann.errors().length > 0) {
            Arrays.stream(ann.errors())
                    .map(c -> (Type) c)
                    .forEach(ep::addErrorType);
        }

        return ep;
    }
}
