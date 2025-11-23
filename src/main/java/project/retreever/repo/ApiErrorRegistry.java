package project.retreever.repo;

import project.retreever.domain.model.ApiError;
import project.retreever.endpoint.resolver.ApiErrorResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ApiErrorRegistry extends DocRegistry<ApiError> {

    private final ApiErrorResolver apiErrorResolver;

    public ApiErrorRegistry(ApiErrorResolver apiErrorResolver) {
        this.apiErrorResolver = apiErrorResolver;
    }

    /**
     * Accepts a list of handler methods, resolves ApiErrors,
     * registers them, and returns the refs.
     */
    public List<String> registerApiErrors(List<Method> methods) {

        List<ApiError> errors = apiErrorResolver.resolve(methods);

        return errors.stream()
                .map(this::registerApiError)
                .toList();
    }

    /**
     * Registers a single ApiError by its exception class name.
     */
    public String registerApiError(ApiError apiError) {

        String ref = apiError.getExceptionName();

        if (!contains(ref)) {
            add(ref, apiError);
        }

        return ref;
    }

    /**
     * Accepts an array of exception classes declared on @ApiEndpoint(errors={})
     * Returns the list of refs **only if** they are already registered.
     */
    public List<String> getErrorRefs(Class<? extends Throwable>[] exceptionTypes) {

        List<String> refs = new ArrayList<>();

        if (exceptionTypes == null) {
            return refs;
        }

        for (Class<? extends Throwable> ex : exceptionTypes) {
            String key = ex.getName();
            if (contains(key)) {
                refs.add(key);
            }
        }

        return refs;
    }
}
